package com.raccoon.findfirstcaller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class XmlToFindFirstCaller extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();

        if (project == null) {
            return;
        }

        if (DumbService.isDumb(project)) {
            // 인덱싱이 완료될 때까지 대기하거나, 작업을 취소하거나, 사용자에게 알림을 표시
            Messages.showDialog(project, "인덱싱이 완료 후 다시 실행 하세요.", "인덱싱 중", new String[]{"OK"}, 0, null);
            return;
        }

        Task.Backgroundable task = new BackgroundTask(project, "처리 중...");
        ProgressManager.getInstance().run(task);

    }

    private static class BackgroundTask extends Task.Backgroundable {
        public BackgroundTask(Project project, String title) {
            super(project, title);
        }

        @Override
        public void run(ProgressIndicator indicator) {
            Project project = getProject();
            Path csvFilePath = new FileInfo().getXmlSavePath().resolve(project.getName() + "_xml.csv");

            try (BufferedReader reader = Files.newBufferedReader(csvFilePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 각 라인을 쉼표로 분리
                    String[] fields = line.split(",");

                    // 필드 검증 및 처리
                    if (fields.length >= 4) {
                        String moduleName = fields[0].replaceAll("\"", "");
                        String cud = fields[1].replaceAll("\"", "");
                        String className = fields[2].replaceAll("\"", "");
                        String methodName = fields[3].replaceAll("\"", "");

                        findFirstCallerProcess(moduleName, cud, className, methodName, project);
                    }
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        private void findFirstCallerProcess(String moduleName, String cud, String className, String methodName, Project project) {
            if (cud.equals("insert") || cud.equals("update") || cud.equals("delete")) {
                ReadAction.run(() -> {
                    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
                    if (psiClass != null) {
                        for (PsiMethod psiMethod : psiClass.getMethods()) {
                            if (methodName.equals(psiMethod.getName())) {
                                Set<CallerInfo> callerInfos = new FindFirstCaller().getCallerInfos(psiMethod, project);
                                new FileInfo().saveCsvFile(callerInfos, true, psiMethod, moduleName);
                            }
                        }
                    }
                });
            }
        }
    }
}

