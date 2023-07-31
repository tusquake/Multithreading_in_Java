class ThreadPriorityDemo extends Thread {

    public void run() {
        System.out.println("child thread");
        System.out.println("Child Thread Priority : " + Thread.currentThread().getPriority());
    }

    public static void main(String[] args) {
        System.out.println("Main Thread old Priority : " + Thread.currentThread().getPriority());
        Thread.currentThread().setPriority(MAX_PRIORITY);
        System.out.println("Main Thread new Priority : " + Thread.currentThread().getPriority());

        ThreadPriorityDemo t = new ThreadPriorityDemo();
        t.setPriority(MIN_PRIORITY);
        t.start();

        ThreadPriorityDemo t1 = new ThreadPriorityDemo();
        t1.setPriority(NORM_PRIORITY);
        t1.start();

        ThreadPriorityDemo t2 = new ThreadPriorityDemo();
        t2.setPriority(8);
        t2.start();
    }
}