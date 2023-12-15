package com.raccoon.findfristcaller;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ResultsDialog extends JDialog {
    private final JTable table;

    public ResultsDialog() {
        setTitle("Search Results");
        setModal(true);
        setSize(600, 400);
        setLocationRelativeTo(null);

        table = new JBTable();
        JScrollPane scrollPane = new JBScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // 테이블 모델 설정
        String[] columnNames = {"Class Name", "Method Name", "URL"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        table.setModel(model);
    }

    public void setResults(Object[][] data) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (Object[] row : data) {
            model.addRow(row);
        }
    }
}
