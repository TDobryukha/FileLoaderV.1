package org.example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class FileLoaderApplicationV1 {

    static String fileName = null;
    static String saveDirectory = null;
    static int numberOfThreads = -1;
    static int rateLimit = -1;

    public static void main(String[] args) {
        boolean success = false;
        Scanner scn = new Scanner(System.in);

        while (!success) {
            if (fileName == null) {
                fileName = getFileName(scn);
            }
            if (saveDirectory == null) {
                saveDirectory = getDirectory(scn);
            }
            if (numberOfThreads < 0) {
                numberOfThreads = getNumberOfThreads(scn);
            }
            if (rateLimit < 0) {
                rateLimit = getRateLimit(scn);
            }
            success = load(fileName, saveDirectory, numberOfThreads, rateLimit);
        }
    }

    public static String getFileName(Scanner scn) {
        System.out.println("Введите полное имя файла со списком URL для загрузки. ");
        System.out.println("Для выхода из программы введите -1");
        String fileName = scn.nextLine();
        if ("-1".equals(fileName)) {
            System.exit(0);
        }
        return fileName;
    }

    public static String getDirectory(Scanner scn) {
        System.out.println("Введите полный путь к каталогу для загрузки файлов. Если каталога не существует, он будет создан.");
        System.out.println("Для выхода из программы введите -1");
        String saveDirectory = scn.nextLine();
        if ("-1".equals(saveDirectory)) {
            System.exit(0);
        }
        return saveDirectory;
    }

    public static int getNumberOfThreads(Scanner scn) {
        System.out.println("Введите количество потоков");
        System.out.println("Для выхода из программы введите -1");
        int numberOfThreads = scn.nextInt();
        scn.nextLine();
        if (numberOfThreads == -1) {
            System.exit(0);
        }
        return numberOfThreads;
    }

    public static int getRateLimit(Scanner scn) {
        System.out.println("Укажите ограничение на скорость загрузки файлов в КБ");
        System.out.println("Для выхода из программы введите -1");
        int rateLimit = scn.nextInt();
        scn.nextLine();
        if (rateLimit == -1) {
            System.exit(0);
        }
        return rateLimit;
    }

    public static boolean load(String fileName_arg, String saveDirectory_arg, int numberOfThreads_arg, int rateLimit_arg) {
        try {
            new FileLoaderService(fileName_arg, saveDirectory_arg, numberOfThreads_arg, rateLimit_arg).load();
            return true;
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            fileName = null;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            saveDirectory = null;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            saveDirectory = null;
            numberOfThreads = -1;
            rateLimit = -1;
        }
        return false;
    }
}

