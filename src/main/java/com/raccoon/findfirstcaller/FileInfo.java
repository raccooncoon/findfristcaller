package com.raccoon.findfirstcaller;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

public class FileInfo {

    public String generateSavePath(String defaultFileName, String projectName) {
        String fileName = projectName + "_" + defaultFileName; // 파일 이름을 프로젝트 이름으로 설정합니다.

        Path directoryPath = Paths.get(System.getProperty("user.home"), "검색 결과");
        java.io.File directory = directoryPath.toFile();

        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directoryPath.resolve(fileName).toString();
    }

    public void saveCsvFile(Set<CallerInfo> callers, boolean append, PsiMethod selectedMethod, String moduleName) {
        String filePath = new FileInfo().generateSavePath("result.csv", selectedMethod.getProject().getName());
        // 파일이 존재하지 않거나, 새로 쓰기 모드인 경우 헤더를 추가합니다.
        boolean writeHeader = !new File(filePath).exists() || !append;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, append))) {
            // 새 파일이거나 새로 쓰기 모드인 경우에만 헤더를 작성합니다.
            if (writeHeader) {
                writer.write("Module Name, Class Name,Method Name,URL,Selected Class Name,Selected Method Name" + System.lineSeparator());
            }

            for (CallerInfo caller : callers) {
                writeCallerInfo(writer, caller, selectedMethod, moduleName);
            }
        } catch (IOException ex) {
            throw new RuntimeException("파일 작성 중 오류 발생", ex);
        }
    }

    public void saveCsvFile(Set<CallerInfo> callers, boolean append, PsiMethod selectedMethod) {
        String filePath = new FileInfo().generateSavePath("result.csv", selectedMethod.getProject().getName());
        // 파일이 존재하지 않거나, 새로 쓰기 모드인 경우 헤더를 추가합니다.
        boolean writeHeader = !new File(filePath).exists() || !append;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, append))) {
            // 새 파일이거나 새로 쓰기 모드인 경우에만 헤더를 작성합니다.
            if (writeHeader) {
                writer.write("Module Name, Class Name,Method Name,URL,Selected Class Name,Selected Method Name" + System.lineSeparator());
            }

            for (CallerInfo caller : callers) {
                writeCallerInfo(writer, caller, selectedMethod);
            }
        } catch (IOException ex) {
            throw new RuntimeException("파일 작성 중 오류 발생", ex);
        }
    }

    private void writeCallerInfo(BufferedWriter writer, CallerInfo caller, PsiMethod selectedMethod, String moduleName) throws IOException {
        String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                moduleName,
                caller.getPsiClass().getQualifiedName(),
                caller.getPsiMethod().getName(),
                caller.getUrl(),
                Objects.requireNonNull(selectedMethod.getContainingClass()).getQualifiedName(),
                selectedMethod.getName()
        );
        writer.write(line);
    }

    private void writeCallerInfo(BufferedWriter writer, CallerInfo caller, PsiMethod selectedMethod) throws IOException {
        String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                caller.getPsiClass().getQualifiedName(),
                caller.getPsiMethod().getName(),
                caller.getUrl(),
                Objects.requireNonNull(selectedMethod.getContainingClass()).getQualifiedName(),
                selectedMethod.getName()
        );
        writer.write(line);
    }

    public void getSave(Node node, Project project, Module module){

        String fileName = project.getName() + "_xml.csv"; // 파일 이름을 프로젝트 이름으로 설정합니다.

        java.io.File directory = getXmlSavePath().toFile();

        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getXmlSavePath().resolve(fileName).toString(),true))) {

            String moduleName = module.getName();

            IntStream.range(0, node.getChildNodes().getLength())
                    .mapToObj(node.getChildNodes()::item)
                    .filter(childNode -> childNode.getNodeType() == Node.ELEMENT_NODE)
                    .forEach(childNode -> {
                        String cud = childNode.getNodeName();
                        String namespace = getAttributeValue(node, "namespace");
                        String mapperId = getAttributeValue(childNode, "id");
                        String textContent = childNode.getTextContent().replaceAll("\"",",");

                        String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                                moduleName,
                                cud,
                                namespace,
                                mapperId,
                                textContent
                        );

                        try {
                            writer.write(line);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

        } catch (IOException ex) {
            throw new RuntimeException("파일 작성 중 오류 발생", ex);
        }
    }

    private String getAttributeValue(Node node, String attributeName) {
        Node attribute = node.getAttributes().getNamedItem(attributeName);
        return attribute != null ? attribute.getNodeValue() : "";
    }
    @NotNull
    public Path getXmlSavePath() {
        return Paths.get(System.getProperty("user.home"), "검색 결과");
    }

    public void deleteFilesInDirectory(Path directoryPath) {
        File directory = new File(directoryPath.toString());
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles(); // 디렉토리 내의 모든 파일을 가져옵니다.
            if (files != null) {
                for (File file : files) {
                    file.delete(); // 각 파일을 삭제합니다.
                }
            }
        }
    }
}
