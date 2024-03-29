package com.raccoon.findfirstcaller;

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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Set;

public class ResultsDialog extends JDialog {
    private final JTable table;

    public ResultsDialog(PsiMethod selectedMethod, Set<CallerInfo> callers) {
        setTitle("검색 메소드 : " + selectedMethod.getName() + "     카운트 : " + callers.size());
        setModal(true);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        table = new JBTable();
        JScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // 테이블 모델 설정
        String[] columnNames = {"모듈 이름", "검색 메소드", "클래스", "메소드", "URL", "xmlTagText"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        table.setModel(model);

        // 컬럼 이름을 가운데 정렬하는 코드 추가
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(JLabel.CENTER);
            }
        });

        // 컬럼 너비 설정
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(500);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getColumnModel().getColumn(4).setPreferredWidth(500);
        table.getColumnModel().getColumn(5).setPreferredWidth(200);

        // 닫기 버튼 추가
        JButton closeButton = new JButton("닫기");
        closeButton.addActionListener(e -> {
            dispose(); // 닫기 버튼을 클릭하면 다이얼로그를 닫음
        });

        // ESC 키를 눌렀을 때 다이얼로그를 닫도록 KeyListener 추가
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose(); // ESC 키를 누르면 다이얼로그를 닫음
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        // 다이얼로그가 포커스를 받을 수 있도록 설정
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);


        // 클립보드 복사 버튼 추가
        JButton copyButton = new JButton("클립보드 복사");
        copyButton.addActionListener(e -> {
            copyToClipboard(); // 클립보드 복사 버튼을 클릭하면 클립보드에 복사
        });

        // 저장 버튼 추가
        JButton saveButton = new JButton("csv 새로 저장");
        saveButton.addActionListener(e -> saveToFile(callers, false, selectedMethod));

        // 추가 버튼 추가
        JButton addButton = new JButton("csv 추가");
        addButton.addActionListener(e -> saveToFile(callers, true, selectedMethod));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        buttonPanel.add(copyButton);
//        buttonPanel.add(saveButton);
//        buttonPanel.add(addButton);
        add(buttonPanel, BorderLayout.SOUTH);

    }

    private void saveToFile(Set<CallerInfo> callers, boolean append, PsiMethod selectedMethod) {
        // FindFirstCaller 클래스의 saveFile 메소드 호출
        new FileInfo().saveCsvFile(callers, append, selectedMethod);
        dispose();
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
