package com.raccoon.findfirstcaller;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Set;

public class XmlToFindFirstCaller extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            Task.Backgroundable task = new BackgroundTask(project, "처리 중...");
            ProgressManager.getInstance().run(task);
        }
    }

    private static class BackgroundTask extends Task.Backgroundable {
        public BackgroundTask(Project project, String title) {
            super(project, title);
        }

        @Override
        public void run(ProgressIndicator indicator) {
            Project project = getProject();
            try (Reader reader = Files.newBufferedReader(new FileInfo().getXmlSavePath().resolve(project.getName() + "-xml.csv"))) {
                try (CSVParser csvParser = CSVParser.parse(reader, CSVFormat.DEFAULT)) {
                    for (CSVRecord csvRecord : csvParser) {
                        String moduleName = csvRecord.get(0);
                        String cud = csvRecord.get(1);
                        String className = csvRecord.get(2);
                        String methodName = csvRecord.get(3);

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

