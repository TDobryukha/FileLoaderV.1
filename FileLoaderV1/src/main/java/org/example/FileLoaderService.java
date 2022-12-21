package org.example;

import com.google.common.util.concurrent.RateLimiter;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileLoaderService {
    private static final int DEFAULT_NUMBER_OF_THREADS = 2;
    private static final String DEFAULT_SAVE_DIRECTORY = "D:/Downloads/";
    private static final int DEFAULT_RATE_LIMIT = 50000; // inputStream.read в FileLoader загружает по 1кб за итерацию. rateLimit = 50000 ограничивает загрузку до 50 мб/сек
    private final List<String> urlList;
    private final String saveDirectory;

    private final int numberOfThreads;
    private final int rateLimit;


    public FileLoaderService(String fileName) throws IOException, IllegalArgumentException {
        this(fileName, DEFAULT_SAVE_DIRECTORY, DEFAULT_NUMBER_OF_THREADS, DEFAULT_RATE_LIMIT);
    }

    public FileLoaderService(String fileName, String saveDirectory, int numberOfThreads, int rateLimit) throws IOException, IllegalArgumentException {
        checkArguments(fileName, saveDirectory, numberOfThreads, rateLimit);
        this.urlList = getUrlListFromFile(fileName);
        this.saveDirectory = saveDirectory;
        this.numberOfThreads = numberOfThreads;
        this.rateLimit = rateLimit;
    }

    private static List<String> getUrlListFromFile(String fileName) {
        List<String> filesNameList = null;
        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            filesNameList = lines.collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return filesNameList.stream().filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    private static void checkArguments(String fileName, String saveDirectory, int numberOfThreads, int rateLimit) throws IOException, IllegalArgumentException {
        if (saveDirectory.isBlank() || numberOfThreads <= 0 || rateLimit <= 0) {
            throw new IllegalArgumentException("Путь к папке для загрузке не должен быть пустым, число потоков и ограничение на скорость загрузки должны быть больше 0");
        }
        if (fileName.isBlank() || Files.notExists(Paths.get(fileName))) {
            throw new FileNotFoundException("Ошибка!!! Файл со списком ссылок не найден: " + fileName);
        }
        Path path = Paths.get(saveDirectory);
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new IOException("Ошибка!!! Ошибка создания папки: " + saveDirectory);
            }
        }
    }


    public void load() {
        if (this.urlList.isEmpty()) {
            System.out.println("Список файлов для загрузки пустой.");
            return;
        }
        Semaphore semaphore = new Semaphore(this.numberOfThreads); //ограничивает число одновременно запущенных потоков
        CountDownLatch cdl = new CountDownLatch(this.urlList.size()); // ожидает, пока завершат работу все потоки по загрузке файлов
        RateLimiter limiter = RateLimiter.create(rateLimit); // ограничивает скорость загрузки
        Iterator<String> iterator = this.urlList.iterator();
        long startTime = System.currentTimeMillis();

        Thread thread = null;
        while (iterator.hasNext()) {
            String url = iterator.next();
            thread = new Thread(new FileLoader(url, this.saveDirectory, semaphore, cdl, limiter));
            thread.start();
        }
        try {
            cdl.await();
        } catch (InterruptedException exc) {
            exc.printStackTrace();
        }
        System.out.printf("Затрачено времени %d с.", (System.currentTimeMillis() - startTime) / 1000);
    }

    class FileLoader implements Runnable {
        private final String url;
        private final String saveDirectory;
        Semaphore semaphore;
        CountDownLatch cdl;
        RateLimiter rateLimiter;

        private FileLoader(String url, String saveDirectory, Semaphore semaphore, CountDownLatch cdl, RateLimiter rateLimiter) {
            this.url = url;
            this.saveDirectory = saveDirectory;
            this.semaphore = semaphore;
            this.cdl = cdl;
            this.rateLimiter = rateLimiter;
        }

        @Override
        public void run() {
            try (BufferedInputStream inputStream = new BufferedInputStream(new URL(this.url).openStream());
                 FileOutputStream fileOS = new FileOutputStream(this.saveDirectory + this.url.substring(this.url.lastIndexOf("/")))) {
                semaphore.acquire();
                System.out.println("Началась загрузка файла: " + this.url);
                byte[] data = new byte[1024];
                int byteContent;
                while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                    this.rateLimiter.acquire();
                    fileOS.write(data, 0, byteContent);
                }

                System.out.println("Файл загружен:           " + this.url);
            } catch (IOException e) {
                System.out.println("Ошибка загрузки:  " + this.url);
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            semaphore.release();
            cdl.countDown();
        }
    }
}
