/**
 * 
 */
package net.azib.ipscan.gui;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;

import net.azib.ipscan.config.Config;
import net.azib.ipscan.config.DimensionsConfig;
import net.azib.ipscan.config.Labels;
import net.azib.ipscan.core.ScanningResult;
import net.azib.ipscan.core.ScanningResultList;
import net.azib.ipscan.core.ScanningSubject;
import net.azib.ipscan.fetchers.Fetcher;
import net.azib.ipscan.fetchers.FetcherRegistry;
import net.azib.ipscan.gui.MainMenu.ColumnsMenu;
import net.azib.ipscan.gui.actions.ColumnsActions;
import net.azib.ipscan.gui.actions.CommandsActions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Table of scanning results.
 * TODO: separate fetcher selection logic to some other calls (the registry?)
 * 
 * @author anton
 */
public class ResultTable extends Table {
		
	private ScanningResultList scanningResults;
	
	private Image[] images = new Image[4];

	private String feederInfo;

	public ResultTable(Composite parent, ColumnsMenu columnsMenu, FetcherRegistry fetcherRegistry, ScanningResultList scanningResultList) {
		super(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		this.scanningResults = scanningResultList;
		
		setHeaderVisible(true);
		setLinesVisible(true);
		
		Listener columnClickListener = new ColumnsActions.ColumnClick(columnsMenu);
		Listener columnResizeListener = new Listener() {
			public void handleEvent(Event event) {
				// save column width
				TableColumn column = (TableColumn) event.widget;
				Config.getDimensionsConfig().setColumnWidth(column.getText(), column.getWidth());
			}
		};
		
		DimensionsConfig dimensionsConfig = Config.getDimensionsConfig();
		
		List fetchers = fetcherRegistry.getSelectedFetchers();
		for (Iterator i = fetchers.iterator(); i.hasNext();) {
			Fetcher fetcher = (Fetcher) i.next();
			TableColumn tableColumn = new TableColumn(this, SWT.NONE);
			String fetcherName = Labels.getLabel(fetcher.getLabel());
			tableColumn.setWidth(dimensionsConfig.getColumnWidth(fetcherName));
			tableColumn.setText(fetcherName);
			tableColumn.addListener(SWT.Selection, columnClickListener);
			tableColumn.addListener(SWT.Resize, columnResizeListener);
		}
		
		// pre-load button images
		images[ScanningSubject.RESULT_TYPE_UNKNOWN] = new Image(null, Labels.getInstance().getImageAsStream("list.unknown.img"));
		images[ScanningSubject.RESULT_TYPE_DEAD] = new Image(null, Labels.getInstance().getImageAsStream("list.dead.img"));
		images[ScanningSubject.RESULT_TYPE_ALIVE] = new Image(null, Labels.getInstance().getImageAsStream("list.alive.img"));
		images[ScanningSubject.RESULT_TYPE_ADDITIONAL_INFO] = new Image(null, Labels.getInstance().getImageAsStream("list.addinfo.img"));
		
		Listener detailsListener = new Listener() {
			CommandsActions.Details detailsListener = new CommandsActions.Details(ResultTable.this);
			public void handleEvent(Event e) {
				if (e.type == SWT.MouseDoubleClick || e.detail == SWT.TRAVERSE_RETURN) {
					detailsListener.handleEvent(e);
					e.doit = false;
				}
			}
		};
		addListener(SWT.Traverse, detailsListener);
		addListener(SWT.MouseDoubleClick, detailsListener);
		
		addListener(SWT.SetData, new SetDataListener());
		
//		for (int i = 0; i < getColumnCount(); i++) {
//			getColumn(i).add
//		}
		
	}

	protected void checkSubclass() {
		// This method is overriden and does nothing in order to
		// be able to subclass the Table. We are not going to 
		// override anything important, so this should be safe (tm)
	}

	public int addResultsRow(final InetAddress address) {
		if (isDisposed())
			return 0;
		final int index = scanningResults.add(address);
		getDisplay().syncExec(new Runnable() {
			public void run() {
				ResultTable.this.setItemCount(index+1);
			}
		});		
		return index;
	}

	public void updateResults(final int index) {
		if (isDisposed())
			return;
		getDisplay().syncExec(new Runnable() {
			public void run() {
				// redraw the item
				ResultTable.this.clear(index);
			}
		});
	}

	/**
	 * Returns the details about the currently selected IP address
	 * @return
	 */
	public String getIPDetails() {
		int selectedIndex = getSelectionIndex();
		return scanningResults.getResultsAsString(selectedIndex);
	}
	
	public void remove(int[] indices) {
		// we need to remove the elements from our real storage as well
		scanningResults.remove(indices);
		super.remove(indices);
	}

	/**
	 * Initializes a new scan.
	 * (clears all elments, etc)
	 * @param newFeederInfo feeder info of the new feeder/settings
	 */
	public void initNewScan(String newFeederInfo) {
		// initialize new feeder info
		this.feederInfo = newFeederInfo; 
		// remove previous results
		scanningResults.clear();
		// remove all items from the table
		removeAll();
	}
		
	/**
	 * @return the internal ScanningResultList instance, containing the results.
	 */
	public ScanningResultList getScanningResults() {
		return scanningResults;
	}

	private class SetDataListener implements Listener {

		public void handleEvent(Event event) {
			TableItem item = (TableItem)event.item;
			int tableIndex = ResultTable.this.indexOf(item);
			
			ScanningResult scanningResult = scanningResults.getResult(tableIndex);
			List values = scanningResult.getValues();
			String[] resultStrings = new String[values.size()];
			for (int i = 0; i < values.size(); i++) {				
				Object value = values.get(i);
				if (value != null)
					resultStrings[i] = value.toString();
			}			 
			item.setText(resultStrings);
			item.setImage(0, images[scanningResult.getType()]);
		}
		
	}

	/**
	 * @return the feeder info, which used in this scan
	 */
	public String getFeederInfo() {
		return feederInfo;
	}

}
