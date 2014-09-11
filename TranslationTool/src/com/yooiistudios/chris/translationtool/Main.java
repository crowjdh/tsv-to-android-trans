package com.yooiistudios.chris.translationtool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import au.com.bytecode.opencsv.CSVReader;

public class Main {
	
	private static JFileChooser mChooser;
	private static JFrame frame;
	private static JLabel mFileName;
	
	private static ArrayList<CountrySet> mLoadedContent;
	private static String mFileDirectory;

	private static final String DIR_NAME = "translation output";
	private static final String COMMENT_PREFIX = "//";
	
	public static void main(String args[]) {
		mChooser = new JFileChooser();
		
		frame = new JFrame("title...");
		Container container = frame.getContentPane();
		frame.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel top = new JPanel();
		top.setAlignmentX(Component.LEFT_ALIGNMENT);
//		top.setBackground(Color.RED);
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		JButton btn = new JButton("load");
		btn.addActionListener(onLoadBtnClickedListener);
//		btn.setPreferredSize(new Dimension(100, 50));
		top.add(btn);
		mFileName = new JLabel("name : ");
		mFileName.setBackground(Color.CYAN);
		mFileName.setPreferredSize(new Dimension(300, 50));
		top.add(mFileName);
		
		container.add(top);
		
		JPanel bottom = new JPanel();
		bottom.setAlignmentX(Component.LEFT_ALIGNMENT);
		bottom.setBackground(Color.BLUE);
		bottom.setLayout(new FlowLayout());
		btn = new JButton("android");
		btn.addActionListener(onConvertToAndroidClickedListener);
		bottom.add(btn);
		btn = new JButton("ios");
		btn.addActionListener(onConvertToIOSClickedListener);
		bottom.add(btn);
		
		container.add(bottom);
		
		frame.setSize(400, 200);
		frame.setVisible(true);
	}

	private static ActionListener onConvertToAndroidClickedListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			if (mLoadedContent == null) {
				return;
			}
			
			File file = makeDir(mFileDirectory, DIR_NAME);
			
			for (CountrySet set : mLoadedContent) {
				writeAndroidFile(file.getAbsolutePath(), set.folderName, set.makeAndroidTranslatedFile());
			}
		}
	};
	
	private static ActionListener onConvertToIOSClickedListener = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			if (mLoadedContent == null) {
				return;
			}
			
			File file = makeDir(mFileDirectory, DIR_NAME);
			
			for (CountrySet set : mLoadedContent) {
				writeIOSFile(file.getAbsolutePath(), set.folderName, set.makeIOSString());
			}
		}
	};
	
	private static ActionListener onLoadBtnClickedListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			mChooser.addChoosableFileFilter(new FileNameExtensionFilter("csv", "csv"));
			mChooser.addChoosableFileFilter(new FileNameExtensionFilter("tsv", "tsv"));
			File curDir = new File(".");
//			System.out.println(curDir.getAbsolutePath().toString());
			mChooser.setCurrentDirectory(curDir.getAbsoluteFile());
			int returnVal = mChooser.showOpenDialog(frame);

			if(returnVal == JFileChooser.APPROVE_OPTION) {
				File file = mChooser.getSelectedFile();
				System.out.println("You chose to open this file: " +
						file.getAbsolutePath());
				
				try {
					mLoadedContent = readTsv(file.getAbsolutePath());
					mFileDirectory = file.getParent();
					mFileName.setText("name : " + file.getName());
				} catch (InvalidTSVFileException exception) {
					mFileName.setText("error : " + exception.getMessage());
				}
				
//				mFileName.setText("name : " + file.getName());
//				mLoadedContent = readFile(file.getAbsolutePath());
//				mFileDirectory = file.getParent();
			}
		}
	};
	
	private static ArrayList<CountrySet> readTsv(String filePath) throws InvalidTSVFileException{
		try {
			CSVReader reader = new CSVReader(new FileReader(filePath), '\t');
			ArrayList<String[]> sheet = new ArrayList<String[]>(reader.readAll());
			
			//check if there's languages to translate
			if (sheet.size() < 2) {
				//invalid
				throw new InvalidTSVFileException("insufficient row size");
			}
			
			//check column(language count) is more than 1
			String[] directoryNameArr = sheet.remove(0);
			if (directoryNameArr.length < 2) {
				//invalid
				throw new InvalidTSVFileException("insufficient column size");
			}
			
//			int languageLastIdx = -1;
//			for (int i = directoryNameArr.length-1; i > 1; i--) {
//				if (!directoryNameArr[i].equals("")) {
//					languageLastIdx = i;
//					break;
//				}
//			}

			//read directory names
			ArrayList<CountrySet> setList = new ArrayList<CountrySet>();
			for (int i = 1; i < directoryNameArr.length; i++) {
				String dirName = directoryNameArr[i];
				if (dirName.equals("")) {
					dirName = i + "th translation";
				}
				setList.add(new CountrySet(dirName));
			}
			
			//read contents
			for (int rowCnt = 0; rowCnt < sheet.size(); rowCnt++) {
				String[] row = sheet.get(rowCnt);
				for (int colCnt = 1; colCnt < directoryNameArr.length+1; colCnt++) {
					System.out.println("colCnt : " + colCnt);
					if ((colCnt-1) >= setList.size()) {
						continue;
					}
					CountrySet set = setList.get(colCnt-1);
					String content = row[colCnt];
					
					String trimmedKey = row[0].trim();
					String trimmedContent = content.trim();
					
					if (trimmedContent.equals("")) {
						set.values.add(null);
					} else if (trimmedContent.startsWith(COMMENT_PREFIX)) {
						Comment comment = new Comment(trimmedContent);
						set.values.add(comment);
					} else {
						LanguageElement element = new LanguageElement(trimmedKey, row[colCnt]);
						set.values.add(element);
					}
				}
			}
			
			return setList;
			
//			while ((nextLine = reader.readNext()) != null) {
//				// nextLine[] is an array of values from the line
//				for (int col = 0; col < nextLine.length; col++) {
//					System.out.println(nextLine[col]);
//				}
//			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
//	private static ArrayList<CountrySet> readFile(String name) {
//		String csvFile = name;
//		BufferedReader br = null;
//		String line = "";
//		String cvsSplitBy = ",";
//
//		try {
//
////			br = new BufferedReader(new FileReader(csvFile));
//			br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), "UTF8"));
////			br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile)));
//			
//			//read 1st line
//			line = br.readLine();
//			String[] contents = line.split(cvsSplitBy);
//			
//			ArrayList<CountrySet> setList = new ArrayList<CountrySet>();
//			
//			for (int i = 1; i < contents.length; i++) {
//				setList.add(new CountrySet(contents[i]));
//			}
//			
//			while ((line = br.readLine()) != null) {
//				String newLine = line.replaceAll(",,", ", ,");
//				if (newLine.charAt(newLine.length()-1) == ',') {
//					newLine += " ";
//				}
//				// use comma as separator
//				contents = newLine.split(cvsSplitBy);
//
//				for (int i = 1; i < contents.length; i++) {
//					LanguageElement element = new LanguageElement(contents[0], contents[i]);
//					
//					CountrySet set = setList.get(i-1);
//					set.values.add(element);
//				}
//			}
//			
//			return setList;
//
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			if (br != null) {
//				try {
//					br.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		return null;
//	}
	
	private static File makeDir(String path, String dirName) {
		File theDir = new File(path, dirName);

		// if the directory does not exist, create it
		if (!theDir.exists()) {
			System.out.println("creating directory: " + theDir.getAbsolutePath());
			boolean result = theDir.mkdir();  

			if(result) {    
				System.out.println("directory created");  
			}
		}
		return theDir;
	}
	private static void writeAndroidFile(String directoryPath, String folderName, String content) {
		writeFile(directoryPath, folderName, content, "strings.xml");
	}
	private static void writeIOSFile(String directoryPath, String folderName, String content) {
		writeFile(directoryPath, folderName, content, "Localizable.strings");
	}
	private static void writeFile(String directoryPath, String folderName, String content, String fileName) {
		try {
			File dir = makeDir(directoryPath, folderName);
			File file = new File(dir, fileName);
//			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
			//
			out.write(content);
			out.close();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
	
	static class CountrySet {
		String folderName;
		ArrayList<Row> values;
		
		public CountrySet() {
			init();
		}
		public CountrySet(String folderName) {
			init();
			this.folderName = folderName;
		}
		private void init() {
			values = new ArrayList<Row>();
		}
		
		public String makeAndroidTranslatedFile() {
//			String prefix = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n" + 
//					"<resources xmlns:tools=\"http://schemas.android.com/tools\">" + "\n";
			String prefix = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n" + 
					"<resources>" + "\n";
			StringBuilder builder = new StringBuilder(prefix);
			for (Row item : values) {
				if (item != null) {
					builder.append("\t").append(item.toAndroidString()).append("\n");
				} else {
					builder.append("\n");
				}
			}
			
			builder.append("</resources>");
			return builder.toString();
		}
		public String makeIOSString() {
			StringBuilder builder = new StringBuilder();
			for (Row item : values) {
				builder.append(item.toIOSString()).append("\n");
			}
			
			return builder.toString();
		}
	}
	static abstract class Row {
		public abstract String toAndroidString();
		public abstract String toIOSString();
	}
	
	static class Comment extends Row {
		private String mComment;
		
		public Comment(String comment) {
			mComment = comment;
		}
		
		@Override
		public String toAndroidString() {
			String commentContent = mComment.substring(COMMENT_PREFIX.length()).trim();
			return "<!-- " + commentContent + " -->";
		}
		@Override
		public String toIOSString() {
			return null;
		}
	}
	
	static class LanguageElement extends Row {
		private String mKey;
		private String mTranslatedString;
		
		private static final String ANDROID_PREFIX_HEADING = "<string name=\"";
		private static final String ANDROID_PREFIX_TAILING = "\">";
		private static final String ANDROID_POSTFIX = "</string>";
		
		public LanguageElement(String key, String translatedString) {
			this.mKey = key;
			if (translatedString.equals(" ")) {
				this.mTranslatedString = "";
			}
			else {
				this.mTranslatedString = translatedString;
			}
		}
		
		@Override
		public String toAndroidString() {
			return configAndroidTranslatedString(ANDROID_PREFIX_HEADING + mKey + ANDROID_PREFIX_TAILING + mTranslatedString + ANDROID_POSTFIX);
		}

		@Override
		public String toIOSString() {
			return "\"" + mKey + "\" = \"" + mTranslatedString + "\";";
		}
		
		private static String configAndroidTranslatedString(String str) {
			return str.replaceAll("&", "&amp;");//.replaceAll("\\.\\.\\.", "&#8230;");
		}
	}

	static class InvalidTSVFileException extends Exception {
		public InvalidTSVFileException(String msg) {
			super(msg);
		}
	}
}
