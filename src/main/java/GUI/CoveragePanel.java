package GUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class CoveragePanel extends JPanel {

    private String proteinSeq;
    private int wordNumOneLine;
    private Object[] proteinPeptideObject;
    private Object[] proteinMappedPeptideObject;
    private Font iBaseFont = new Font(Font.MONOSPACED, Font.PLAIN, 16);

    public CoveragePanel(String proteinSeq, Object[] proteinPeptideObject, Object[] proteinMappedPeptideObject, int width, int height){
        //setPreferredSize(new Dimension(500,500));
        setBackground(Color.white);
        this.proteinSeq = proteinSeq;
        this.proteinPeptideObject = proteinPeptideObject;
        this.proteinMappedPeptideObject = proteinMappedPeptideObject;
        setSize(width, height, proteinSeq.length());
    }

    private void setSize(int width, int height, int seqLen){
        width -= 55;
        wordNumOneLine = (int) Math.floor(width/15);
        int wordLine = (int) Math.ceil(seqLen/wordNumOneLine) + 1;

        setPreferredSize(new Dimension(width,wordLine*17));

    }

    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;

        paintOne(g2);

    }

    private void paintOne(Graphics2D g2){
        g2.setFont(iBaseFont);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Drawing offsets.
        int yLocation = 2;
        int xLocation = 1;

        ArrayList<Integer> mappedIndex = new ArrayList<>();
        HashMap<Integer, Color> modificationMap = new HashMap<>();
        if (proteinPeptideObject != null){
            mappedIndex = (ArrayList<Integer>) proteinPeptideObject[0];
            modificationMap = (HashMap<Integer, Color>) proteinPeptideObject[1];
        }

        ArrayList<Integer> mappedProteinIndex = new ArrayList<>();
        if (proteinMappedPeptideObject != null){
            mappedProteinIndex = (ArrayList<Integer>) proteinMappedPeptideObject[0];
        }

        int lFontHeight = g2.getFontMetrics().getHeight();
        for (int i = 0; i < proteinSeq.length(); i++) {

            String aa = proteinSeq.substring(i, i+1);

            xLocation += 15;
            if (i % wordNumOneLine == 0){
                yLocation += 16;
                xLocation = 1;
            }

            if (mappedIndex.contains(i)){
                g2.setColor(new Color(48, 238, 94, 50));
                g2.fillRect( xLocation-1, yLocation-15, 15, 15);
            }
            if (mappedProteinIndex.contains(i)){
                g2.setColor(Color.LIGHT_GRAY);
                //g2.setColor(new Color(234, 14, 14, 50));
                g2.fillRect( xLocation-1, yLocation-15, 15, 15);
            }

            if (modificationMap.containsKey(i)){
                g2.setColor(modificationMap.get(i));
                g2.fillRect( xLocation-15, yLocation, 10, 2);
            }

            g2.setColor(Color.black);
            g2.drawString(aa, xLocation, yLocation);

        }
    }

}
