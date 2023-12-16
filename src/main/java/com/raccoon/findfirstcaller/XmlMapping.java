package com.raccoon.findfirstcaller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.SneakyThrows;
import org.apache.ibatis.parsing.XPathParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class XmlMapping extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 외부 DTD 액세스 허용 설정
        System.setProperty("javax.xml.accessExternalDTD", "all");
        Project project = e.getProject();

        // 실행 전 기존 파일 삭제
        new FileInfo().deleteFilesInDirectory(new FileInfo().getXmlSavePath());

        if (project != null) {
            Module[] modules = ModuleManager.getInstance(project).getModules();
            for (Module module : modules) {
                String moduleName = module.getName();
                System.out.println("moduleName = " + moduleName);
                VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
                for (VirtualFile sourceRoot : sourceRoots) {
                    getXmlFileList(sourceRoot.getPath(), moduleName);
                }
            }
        }
    }

    @SneakyThrows
    private void getXmlFileList(String folderPath, String moduleName) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(folderPath))) {
            pathStream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                    .map(Path::toFile)
                    .flatMap(this::getXNodeList)
                    .forEach(x -> new FileInfo().getSave(x, moduleName));
        }
    }

    private Stream<XnodeRecord> getXNodeList(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            XPathParser parser = new XPathParser(fileInputStream, false, null, null);
            return Stream.of("/mapper", "/sqlMap")
                    .flatMap(expression -> parser.evalNodes(expression).stream())
                    .flatMap(nodes -> nodes.getChildren().stream())
                    .map(node -> new XnodeRecord(parser.evalString("//" + node.getPath() + "[@id='" + node.getStringAttribute("id") + "']"), file, node));
        } catch (IOException | RuntimeException e) {
            // 파일을 읽거나 파싱하는 중 발생한 오류를 기록하고 무시합니다.
            System.err.println("Error processing file: " + file.getPath() + " - " + e.getMessage());
            return Stream.empty(); // 오류가 발생한 파일은 무시하고 빈 스트림을 반환합니다.
        }
    }
}
