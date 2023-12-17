package com.raccoon.findfirstcaller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class XmlMapping extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 외부 DTD 액세스 허용 설정
        System.setProperty("javax.xml.accessExternalDTD", "all");
        Project project = e.getProject();

        if (project == null) {
            return;
        }

        if (DumbService.isDumb(project)) {
            // 인덱싱이 완료될 때까지 대기하거나, 작업을 취소하거나, 사용자에게 알림을 표시
            Messages.showDialog(project, "인덱싱이 완료 후 다시 실행 하세요.", "인덱싱 중", new String[]{"OK"}, 0, null);
            return;
        }

        // 실행 전 기존 파일 삭제
        new FileInfo().deleteFilesInDirectory(new FileInfo().getXmlSavePath());

        Task.Backgroundable task = new Task.Backgroundable(project, "Processing XML Files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Module[] modules = ModuleManager.getInstance(project).getModules();
                for (Module module : modules) {
                    VirtualFile projectBaseDir = project.getBaseDir();
                    if (projectBaseDir != null) {
                        getXmlFileList(projectBaseDir.getPath(), project, module);
                    }
                }
            }
        };

        ProgressManager.getInstance().run(task);
    }

    @SneakyThrows
    private void getXmlFileList(String folderPath, Project project, Module module) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(folderPath))) {
            pathStream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                    .map(Path::toFile)
                    .flatMap(this::getXNodeList)
                    .forEach(node -> new FileInfo().getSave(node, project, module));
        }
    }

    private Stream<Node> getXNodeList(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); // 보안 설정
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            document.getDocumentElement().normalize();

            return Stream.of("mapper", "sqlMap")
                    .flatMap(nodeName -> getNodeStream(document, nodeName));

        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.err.println("Error processing file: " + file.getPath() + " - " + e.getMessage());
            return Stream.empty();
        }
    }

    private Stream<Node> getNodeStream(Document document, String nodeName) {
        NodeList nodes = document.getElementsByTagName(nodeName);
        return IntStream.range(0, nodes.getLength())
                .mapToObj(nodes::item)
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE);
    }
}
