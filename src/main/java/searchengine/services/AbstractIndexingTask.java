package searchengine.services;

import java.util.concurrent.RecursiveTask;

public abstract class AbstractIndexingTask extends RecursiveTask<Boolean> {

    public abstract void stopCompute();
}
