package com.easybooking.reservation.test;

public class TestSync {
    private static final Object LOCK = new Object();
    private static int SyncNum = 0;
    private static boolean SyncBool = true;

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                System.out.println("worker begin");
                while (SyncBool) {
                    getAndIncr();
                    Thread.sleep(0);
                }
                System.out.println("worker: " + SyncNum);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                System.out.println("manager begin");
                while (SyncBool)
                    getAndSetBool();
                System.out.println("manager: " + SyncNum);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void getAndIncr() {
        synchronized (LOCK) {
            if (SyncBool) {
                SyncNum++;
            }
        }
    }

    private static void getAndSetBool() {
        synchronized (LOCK) {
            if (SyncNum >= 10_000_000) {
                SyncBool = false;
            }
        }
    }

}
