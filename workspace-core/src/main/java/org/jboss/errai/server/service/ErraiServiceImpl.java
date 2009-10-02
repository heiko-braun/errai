package org.jboss.errai.server.service;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import org.jboss.errai.client.framework.MessageCallback;
import org.jboss.errai.client.rpc.CommandMessage;
import org.jboss.errai.client.rpc.ConversationMessage;
import org.jboss.errai.client.rpc.protocols.MessageParts;
import org.jboss.errai.client.rpc.protocols.SecurityCommands;
import org.jboss.errai.client.rpc.protocols.SecurityParts;
import org.jboss.errai.client.security.CredentialTypes;
import org.jboss.errai.server.Module;
import org.jboss.errai.server.annotations.LoadModule;
import org.jboss.errai.server.bus.MessageBus;
import org.jboss.errai.server.security.auth.AuthorizationAdapter;
import org.jboss.errai.server.security.auth.BasicAuthorizationListener;
import org.jboss.errai.server.security.auth.JAASAdapter;
import org.jboss.errai.server.security.auth.RoleAuthDescriptor;

import java.io.File;
import java.io.IOException;
import static java.lang.Thread.currentThread;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class ErraiServiceImpl implements ErraiService {
    private MessageBus bus;
    private AuthorizationAdapter authorizationAdapter;

    @Inject
    public ErraiServiceImpl(MessageBus bus, AuthorizationAdapter authorizationAdapter) {
        this.bus = bus;
        this.authorizationAdapter = authorizationAdapter;

        init();
    }

    private void init() {
        // just use the simple bus for now.  more integration options to come...
        bus.addGlobalListener(new BasicAuthorizationListener(authorizationAdapter, bus));

        //todo: this all needs to be refactored at some point.
        bus.subscribe(AUTHORIZATION_SVC_SUBJECT, new MessageCallback() {
            public void callback(CommandMessage c) {
                switch (SecurityCommands.valueOf(c.getCommandType())) {
                    case WhatCredentials:
                        /**
                         * Respond with what credentials the authentication system requires.
                         */
                        //todo: we only support login/password for now
                        bus.send(c.get(String.class, SecurityParts.ReplyTo),
                                ConversationMessage.create(SecurityCommands.WhatCredentials, c)
                                        .set(SecurityParts.CredentialsRequired, "Name,Password")
                                        .set(SecurityParts.ReplyTo, AUTHORIZATION_SVC_SUBJECT));
                        break;

                    case AuthRequest:
                        /**
                         * Send a challenge.
                         */
                        authorizationAdapter.challenge(c);
                        break;

                    case EndSession:
                        authorizationAdapter.endSession(c);
                        bus.send(ConversationMessage.create(c).setSubject("LoginClient")
                                .setCommandType(SecurityCommands.SecurityChallenge));
                        break;
                }
            }
        });

        /**
         * A temporary test service to bounce remote messages of.
         */
        bus.subscribe("TestService", new MessageCallback() {
            public void callback(CommandMessage message) {
                System.out.println("yay!");
            }
        });

        /**
         * Some temporary security rules to test the login system.
         */
        RoleAuthDescriptor authRequired = new RoleAuthDescriptor(new String[]{CredentialTypes.Authenticated.name()});
        ((JAASAdapter) authorizationAdapter).addSecurityRule("TestService", authRequired);
        ((JAASAdapter) authorizationAdapter).addSecurityRule("ServerEchoService", authRequired);

        /**
         * The standard ServerEchoService.
         */
        bus.subscribe("ServerEchoService", new MessageCallback() {
            public void callback(CommandMessage c) {
                if (c.hasPart(MessageParts.ReplyTo)) {
                    bus.send(ConversationMessage.create(c));
                }
            }
        });

        loadConfig();

    }

    public void store(CommandMessage message) {
        /**
         * If an authorization adapter is configured, we now allow it to pre-process the request.
         */
        if (authorizationAdapter != null) {
            authorizationAdapter.process(message);
        }

        /**
         * Pass the message off to the messaging bus for handling.
         */
        try {
            bus.sendGlobal(message);
        }
        catch (Throwable t) {
            System.err.println("Message was not delivered.");
            t.printStackTrace();
        }
    }

    private void loadConfig() {

        try {
            ResourceBundle bundle = ResourceBundle.getBundle("errai");
            String modulesToLoad = bundle.getString("workspace.server_modules");

            String[] moduleFQCN = modulesToLoad.split(",");
            @SuppressWarnings({"unchecked"}) Class<Module>[] moduleClass = new Class[moduleFQCN.length];

            try {
                for (int i = 0; i < moduleFQCN.length; i++) {
                    if (moduleClass[i] == null) continue;
                    try {
                        //noinspection unchecked
                        moduleClass[i] = (Class<Module>) Class.forName(moduleFQCN[i]);
                    }
                    catch (Exception e) {
                        throw new RuntimeException("unable to load module: " + moduleClass[i], e);
                    }
                }
            }
            catch (RuntimeException e) {
                throw e;
            }
            catch (Exception e) {
                throw new RuntimeException("error", e);
            }

            try {
                for (Class<Module> clazz : moduleClass) {
                    if (clazz == null) continue;
                    clazz.newInstance().init();
                }
            }
            catch (Exception e) {
                throw new RuntimeException("error loading module: " + e.getMessage(), e);
            }

            try {
                Enumeration<URL> targets = currentThread().getContextClassLoader().getResources("ErraiApp.properties");
                Set<String> loaded = new HashSet<String>();

                while (targets.hasMoreElements()) {
                    findLoadableModules(targets.nextElement(), loaded);
                }

                System.out.println("loaded...");
            }


            catch (IOException e) {
                e.printStackTrace();
            }

        }
        catch (Exception e) {
            throw new RuntimeException("unable to load config", e);
        }
    }

    private void findLoadableModules(URL url, Set<String> loaded) {
        File root = new File(url.getFile()).getParentFile();
        _findLoadableModules(root, root, loaded);
    }

    private void _findLoadableModules(File root, File start, Set<String> loaded) {
        for (File file : start.listFiles()) {
            if (file.isDirectory()) _findLoadableModules(root, file, loaded);
            if (file.getName().endsWith(".class")) {
                try {
                    String FQCN = getCandidateFQCN(root.getAbsolutePath(), file.getAbsolutePath());

                    if (loaded.contains(FQCN)) {
                        return;
                    }
                    else {
                        loaded.add(FQCN);
                    }


                    final Class<? extends Module> clazz = (Class<? extends Module>) Class.forName(FQCN);
                    if (clazz.isAnnotationPresent(LoadModule.class)) {
                        Guice.createInjector(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Module.class).to(clazz);
                                bind(MessageBus.class).toInstance(bus);
                            }
                        }).getInstance(Module.class).init();
                    }
                }
                catch (NoClassDefFoundError e) {
                    // do nothing.
                }
                catch (ExceptionInInitializerError e) {
                    // do nothing.
                }
                catch (UnsupportedOperationException e) {
                    // do nothing.
                }
                catch (ClassNotFoundException e) {
                    // do nothing.
                }
                catch (UnsatisfiedLinkError e) {

                }
//                catch (IllegalAccessException e) {
//                    throw new RuntimeException("Failed to load module", e);
//                }
//                catch (InstantiationException e) {
//                    throw new RuntimeException("Failed to load module", e);
//                }
            }
        }
    }

    private String getCandidateFQCN(String rootFile, String fileName) {
        return fileName.replaceAll("(/|\\\\)", ".")
                .substring(rootFile.length() + 1, fileName.lastIndexOf('.'));
    }



    public MessageBus getBus() {
        return bus;
    }
}