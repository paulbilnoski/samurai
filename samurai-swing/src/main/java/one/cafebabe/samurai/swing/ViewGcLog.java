/*
 * Copyright 2003-2015 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.cafebabe.samurai.swing;

import com.sun.tools.attach.AttachNotSupportedException;
import one.cafebabe.samurai.remotedump.ProcessUtil;
import one.cafebabe.samurai.remotedump.VM;
import one.cafebabe.samurai.remotedump.VirtualMachineUtil;
import one.cafebabe.samurai.util.Configuration;
import one.cafebabe.samurai.util.GUIResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.jvmstat.monitor.MonitorException;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewGcLog {
    private static final Logger logger = LogManager.getLogger();

    private static final GUIResourceBundle resources = GUIResourceBundle.getInstance();

    private final FileHistory fileHistory;
    private final JMenu viewGcLogMenu = new JMenu(resources.getMessage("menu.file.viewGcLogFrom"));

    public ViewGcLog(Configuration config, FileHistory fileHistory) {
        config.apply(this);
        this.fileHistory = fileHistory;
        viewGcLogMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                updateChildMenuItems();
            }

            @Override
            public void menuDeselected(MenuEvent e) {

            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        });
    }

    public JMenu getViewGcLogMenu() {
        return this.viewGcLogMenu;
    }

    private void updateChildMenuItems() {
        try {
            List<VM> currentVms = ProcessUtil.getVMs("localhost");

            for (int i = 0; i < viewGcLogMenu.getItemCount(); i++) {
                LocalProcessMenuItem item = (LocalProcessMenuItem) viewGcLogMenu.getItem(i);
                boolean found = false;
                for (VM vm : currentVms) {
                    if (item.getVm().getPid() == vm.getPid()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    viewGcLogMenu.remove(item);
                }

            }

            for (VM vm : currentVms) {
                boolean found = false;
                for (int i = 0; i < viewGcLogMenu.getItemCount(); i++) {
                    LocalProcessMenuItem item = (LocalProcessMenuItem) viewGcLogMenu.getItem(i);
                    if (item.getVm().getPid() == vm.getPid()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    JMenuItem localProcess = new LocalProcessMenuItem(vm);
                    localProcess.setToolTipText(vm.getFullCommandLine());
                    viewGcLogMenu.add(localProcess);
                }
            }
        } catch (URISyntaxException | MonitorException e) {
            logger.warn("unable to monitor", e);
            e.printStackTrace();
        }
    }

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    ExecutorService executor = Executors.newFixedThreadPool(1);

    class LocalProcessMenuItem extends JMenuItem {
        private static final long serialVersionUID = 2629440395264682598L;
        final VM vm;

        public LocalProcessMenuItem(VM vm) {
            super(String.format("%s %s", vm.getPid(), vm.getFqcn()));
            this.vm = vm;
            addActionListener(e -> executor.execute(() -> {
                try {
                    String gcLogPath = VirtualMachineUtil.getGCLogPath(vm.getPid());
                    if (gcLogPath == null) {
                        Path path = Paths.get(System.getProperty("user.home"),
                                String.format("%s-%d-%s.gc.txt", vm.getFqcn(), vm.getPid(), LocalDateTime.now().format(dateTimeFormatter)));
                        Files.writeString(path, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        Files.writeString(path, "pid: " + vm.getPid() + "\n\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        Files.writeString(path, "FQCN: " + vm.getFqcn() + "\n\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        Files.writeString(path, String.format("Command line:\n%s\n\n", vm.getFullCommandLine()), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        gcLogPath = path.toFile().getAbsolutePath();
                        VirtualMachineUtil.setGCLogPath(vm.getPid(), gcLogPath);
                    }
                    fileHistory.open(new File(gcLogPath));
                } catch (AttachNotSupportedException | IOException e1) {
                    logger.warn("failed to attach pid[{}]",vm.getPid(), e1);
                }
            }));
        }

        public VM getVm() {
            return vm;
        }

    }
}