/*******************************************************************************
 * Copyright (c) 2013 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.cloudifysource.cosmo.orchestrator.integration;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;
import org.cloudifysource.cosmo.config.TestConfig;
import org.cloudifysource.cosmo.dsl.packaging.DSLPackage;
import org.cloudifysource.cosmo.fileserver.config.JettyFileServerConfig;
import org.cloudifysource.cosmo.messaging.config.MockMessageConsumerConfig;
import org.cloudifysource.cosmo.messaging.config.MockMessageProducerConfig;
import org.cloudifysource.cosmo.monitor.Agent;
import org.cloudifysource.cosmo.monitor.ResourceMonitorServer;
import org.cloudifysource.cosmo.monitor.config.ResourceMonitorServerConfig;
import org.cloudifysource.cosmo.orchestrator.integration.config.MockPortKnockerConfig;
import org.cloudifysource.cosmo.orchestrator.integration.config.RuoteRuntimeConfig;
import org.cloudifysource.cosmo.orchestrator.integration.config.TemporaryDirectoryConfig;
import org.cloudifysource.cosmo.orchestrator.workflow.RuoteRuntime;
import org.cloudifysource.cosmo.orchestrator.workflow.RuoteWorkflow;
import org.cloudifysource.cosmo.orchestrator.workflow.config.DefaultRuoteWorkflowConfig;
import org.cloudifysource.cosmo.statecache.config.RealTimeStateCacheConfig;
import org.cloudifysource.cosmo.tasks.config.CeleryWorkerProcessConfig;
import org.cloudifysource.cosmo.tasks.config.EventHandlerConfig;
import org.cloudifysource.cosmo.tasks.config.JythonProxyConfig;
import org.cloudifysource.cosmo.tasks.config.TaskExecutorConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

/**
 * Test requirements:
 *
 * Python packages: celery, paramiko & python-vagrant.
 * Running rabbitmq server.
 *
 * @author Dan Kilman
 * @since 0.1
 */
@ContextConfiguration(classes = { VagrantAndWebserverServiceIT.Config.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class VagrantAndWebserverServiceIT extends AbstractTestNGSpringContextTests {

    /**
     */
    @Configuration
    @Import({
            TemporaryDirectoryConfig.class,
            JettyFileServerForPluginsConfig.class,
            MockMessageConsumerConfig.class,
            MockMessageProducerConfig.class,
            RealTimeStateCacheConfig.class,
            DefaultRuoteWorkflowConfig.class,
            RuoteRuntimeConfig.class,
            ResourceMonitorServerConfig.class,
            MockPortKnockerConfig.class,
            TaskExecutorConfig.class,
            EventHandlerConfig.class,
            JythonProxyConfig.class,
            CeleryWorkerProcessConfig.class
    })
    @PropertySource("org/cloudifysource/cosmo/orchestrator/integration/config/test.properties")
    static class Config extends TestConfig {
    }

    /**
     */
    @Configuration
    public static class JettyFileServerForPluginsConfig extends JettyFileServerConfig {

        @Inject
        private TemporaryDirectoryConfig.TemporaryDirectory temporaryDirectory;

        @PostConstruct
        public void setResourceBase() {
            this.resourceBase = temporaryDirectory.get().getAbsolutePath();
        }
    }

    @Inject
    private RuoteRuntime ruoteRuntime;

    @Inject
    private RuoteWorkflow ruoteWorkflow;

    @Inject
    private ResourceMonitorServer resourceMonitor;

    @Inject
    private TemporaryDirectoryConfig.TemporaryDirectory temporaryDirectory;

    // format is: host:port:id
    @NotNull
    @Value("${cosmo.test.port-knocker.sockets-to-knock}")
    private String[] socketsToKnock;


    @Test(timeOut = 60000 * 5, groups = "vagrant")
    public void testWithVagrantHostProvisionerAndSimpleWebServerInstaller() {
        test("org/cloudifysource/cosmo/dsl/integration_phase1/integration-phase1.yaml");
    }

    @Test(groups = "vagrant")
    public void testWithVagrantHostProvisionerAndRemoteCeleryWorker() {
        test("org/cloudifysource/cosmo/dsl/integration_phase3/integration-phase3.yaml");
    }

    private void test(String dslLocation) {

        // Add resource to be probed by resource monitor
        for (String socket : socketsToKnock) {
            String[] values = socket.split(":");
            Agent agent = new Agent();
            agent.setAgentId(values[2]);
            resourceMonitor.insertFact(agent);
        }

        createZipForPlugin(
                "celery/app/cosmo/cloudify/tosca/artifacts/plugin/diamond_installer/installer",
                temporaryDirectory.get(),
                "diamond-installer.zip");
        copyResourceTarget("diamond_collectors/celeryd/celeryd.py",
                           temporaryDirectory.get(),
                           "celeryd.py");

        final Map<String, Object> workitemFields = Maps.newHashMap();
        workitemFields.put("dsl", dslLocation);

        final Object wfid = ruoteWorkflow.asyncExecute(workitemFields);
        ruoteRuntime.waitForWorkflow(wfid);
    }

    private static void createZipForPlugin(String resourceRoot,
                                           File targetDir, String targetName) {
        final DSLPackage.DSLPackageBuilder packagedPluginBuilder = new DSLPackage.DSLPackageBuilder();
        URL visitorRootUrl = Resources.getResource(resourceRoot);
        final Path visitorRootPath = Paths.get(URI.create("file://" + visitorRootUrl.getPath()));
        try {
            Files.walkFileTree(visitorRootPath, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".pyc")) {
                        return FileVisitResult.CONTINUE;
                    }
                    byte[] content = com.google.common.io.Files.toByteArray(file.toFile());
                    String targetFile = visitorRootPath.relativize(file).toString();
                    packagedPluginBuilder.addFile(targetFile, content);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        packagedPluginBuilder.build().write(new File(targetDir, targetName));
    }

    private static void copyResourceTarget(String resource,
                                           File targetDir,
                                           String fileName) {
        final URL resourceUrl = Resources.getResource(resource);
        try {
            com.google.common.io.Files.copy(new InputSupplier<InputStream>() {
                @Override
                public InputStream getInput() throws IOException {
                  return resourceUrl.openStream();
                }
            }, new File(targetDir, fileName));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
