package mobemu.utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;

public class HCMMComponent extends JComponent {

	private static final long serialVersionUID = 1L;
	private HCMM hcmm;

	public HCMMComponent(HCMM hcmm) {
		this.hcmm = hcmm;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Graphics2D g2d = (Graphics2D) g;

		Color[] colors = { Color.red, Color.blue, Color.green, Color.orange, Color.cyan, Color.darkGray, Color.yellow,
				Color.pink, Color.magenta, Color.gray };

		HCMM.Host[] hosts = hcmm.getHosts();

		for (int i = 0; i < hcmm.getNumHosts(); i++) {
			int x = (int) (hosts[i].currentX * getSize().width / hcmm.getGridWidth());
			int y = (int) (hosts[i].currentY * getSize().height / hcmm.getGridHeight());

			int width = 10;
			int height = 10;

			for (int j = 0; j < hcmm.getNumHosts(); j++) {
				double currentDistance = Math
						.sqrt((hosts[i].currentX - hosts[j].currentX) * (hosts[i].currentX - hosts[j].currentX)
								+ (hosts[i].currentY - hosts[j].currentY) * (hosts[i].currentY - hosts[j].currentY));

				if (currentDistance < hcmm.getRadius() && i < j) {
					int x1 = (int) (hosts[j].currentX * getSize().width / hcmm.getGridWidth());
					int y1 = (int) (hosts[j].currentY * getSize().height / hcmm.getGridHeight());

					g2d.setColor(Color.black);
					g2d.drawLine(x + width / 2, y + height / 2, x1 + width / 2, y1 + height / 2);
				}
			}

			for (int j = 0; j < hcmm.getNumberOfGroups(); j++) {
				for (int k = 0; k < hcmm.getNumberOfMembers()[j]; k++)
					if (i + 1 == hcmm.getGroups()[j][k]) {
						g2d.setColor(colors[j % colors.length]);
						break;
					}
			}

			// g2d.drawOval(x, y, width, height);
			Ellipse2D.Double circle = new Ellipse2D.Double(x, y, width, height);
			g2d.fill(circle);
		}

		// draw the horizontal lines of the grid.
		for (int i = 0; i < hcmm.getNumberOfRows(); i++) {
			g2d.setColor(Color.lightGray);
			g2d.drawLine(0, (int) (i * (double) getSize().height / (double) hcmm.getNumberOfRows()), getSize().width,
					(int) (i * (double) getSize().height / (double) hcmm.getNumberOfRows()));
		}

		// draw the vertical lines of the grid.
		for (int i = 0; i < hcmm.getNumberOfColumns(); i++) {
			g2d.setColor(Color.lightGray);
			g2d.drawLine((int) (i * (double) getSize().width / (double) hcmm.getNumberOfColumns()), 0,
					(int) (i * (double) getSize().width / (double) hcmm.getNumberOfColumns()), getSize().height);
		}
	}
}
