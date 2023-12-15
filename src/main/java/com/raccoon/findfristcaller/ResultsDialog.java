package com.raccoon.findfristcaller;

import com.intellij.psi.PsiMethod;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class ResultsDialog extends JDialog {
    private final JTable table;

    public ResultsDialog(PsiMethod selectedMethod, int count) {
        setTitle("검색 메소드 : " + selectedMethod.getName() + "     카운드 : " + count);
        setModal(true);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        table = new JBTable();
        JScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // 테이블 모델 설정
        String[] columnNames = {"클래스", "메소드", "URL"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        table.setModel(model);

        // 컬럼 이름을 가운데 정렬하는 코드 추가
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(JLabel.CENTER);
            }
        });

        // 닫기 버튼 추가
        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> {
            dispose(); // 닫기 버튼을 클릭하면 다이얼로그를 닫음
        });

        // 클립보드 복사 버튼 추가
        JButton copyButton = new JButton("클립보드 복사");
        copyButton.addActionListener(e -> {
            copyToClipboard(); // 클립보드 복사 버튼을 클릭하면 클립보드에 복사
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(copyButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void setResults(Object[][] data) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (Object[] row : data) {
            model.addRow(row);
        }
    }

    private void copyToClipboard() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        StringBuilder clipboardData = new StringBuilder();

        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                clipboardData.append(model.getValueAt(row, col));
                if (col < model.getColumnCount() - 1) {
                    clipboardData.append("\t"); // 탭으로 열 구분
                }
            }
            clipboardData.append("\n"); // 다음 행으로 이동
        }

        // 클립보드에 데이터 복사
        StringSelection selection = new StringSelection(clipboardData.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }
}
