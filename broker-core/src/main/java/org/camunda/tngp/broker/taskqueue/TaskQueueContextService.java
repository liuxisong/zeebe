package org.camunda.tngp.broker.taskqueue;

import org.camunda.tngp.broker.log.LogConsumer;
import org.camunda.tngp.broker.log.Templates;
import org.camunda.tngp.broker.services.HashIndexManager;
import org.camunda.tngp.broker.taskqueue.log.handler.TaskInstanceHandler;
import org.camunda.tngp.broker.taskqueue.log.handler.TaskInstanceRequestHandler;
import org.camunda.tngp.broker.taskqueue.log.idx.LockedTasksIndexWriter;
import org.camunda.tngp.broker.taskqueue.log.idx.TaskTypeIndexWriter;
import org.camunda.tngp.hashindex.Bytes2LongHashIndex;
import org.camunda.tngp.hashindex.Long2LongHashIndex;
import org.camunda.tngp.log.Log;
import org.camunda.tngp.log.LogReaderImpl;
import org.camunda.tngp.log.LogWriter;
import org.camunda.tngp.log.idgenerator.IdGenerator;
import org.camunda.tngp.servicecontainer.Injector;
import org.camunda.tngp.servicecontainer.Service;
import org.camunda.tngp.servicecontainer.ServiceContext;
import org.camunda.tngp.transport.requestresponse.server.DeferredResponsePool;

public class TaskQueueContextService implements Service<TaskQueueContext>
{
    protected final Injector<Log> logInjector = new Injector<>();
    protected final Injector<IdGenerator> taskInstanceIdGeneratorInjector = new Injector<>();
    protected final Injector<HashIndexManager<Long2LongHashIndex>> lockedTasksIndexServiceInjector = new Injector<>();
    protected final Injector<HashIndexManager<Bytes2LongHashIndex>> taskTypeIndexServiceInjector = new Injector<>();

    protected final Injector<DeferredResponsePool> responsePoolServiceInjector = new Injector<>();

    protected final TaskQueueContext taskQueueContext;

    public TaskQueueContextService(String taskQueueName, int taskQueueId)
    {
        taskQueueContext = new TaskQueueContext(taskQueueName, taskQueueId);
    }

    @Override
    public void start(ServiceContext serviceContext)
    {
        final HashIndexManager<Long2LongHashIndex> lockedTasksIndexManager = lockedTasksIndexServiceInjector.getValue();
        final HashIndexManager<Bytes2LongHashIndex> taskTypeIndexManager = taskTypeIndexServiceInjector.getValue();

        taskQueueContext.setLog(logInjector.getValue());
        taskQueueContext.setTaskInstanceIdGenerator(taskInstanceIdGeneratorInjector.getValue());
        taskQueueContext.setTaskTypePositionIndex(taskTypeIndexManager);

        final Log log = logInjector.getValue();

        final LogWriter logWriter = new LogWriter(log);
        taskQueueContext.setLogWriter(logWriter);

        final Templates templates = Templates.taskQueueLogTemplates();
        final LogConsumer taskProcessor = new LogConsumer(new LogReaderImpl(log), responsePoolServiceInjector.getValue(), templates);

        taskProcessor.addHandler(Templates.TASK_INSTANCE, new TaskInstanceHandler());
        taskProcessor.addHandler(Templates.TASK_INSTANCE_REQUEST, new TaskInstanceRequestHandler(new LogReaderImpl(log), logWriter, lockedTasksIndexManager.getIndex()));

        taskProcessor.addIndexWriter(new TaskTypeIndexWriter(taskTypeIndexManager, templates));
        taskProcessor.addIndexWriter(new LockedTasksIndexWriter(lockedTasksIndexManager, templates));

        taskQueueContext.setLogConsumer(taskProcessor);
    }

    @Override
    public void stop()
    {
        taskQueueContext.getLogConsumer().writeSafepoints();
    }

    @Override
    public TaskQueueContext get()
    {
        return taskQueueContext;
    }

    public Injector<Log> getLogInjector()
    {
        return logInjector;
    }

    public Injector<IdGenerator> getTaskInstanceIdGeneratorInjector()
    {
        return taskInstanceIdGeneratorInjector;
    }

    public Injector<HashIndexManager<Long2LongHashIndex>> getLockedTasksIndexServiceInjector()
    {
        return lockedTasksIndexServiceInjector;
    }

    public TaskQueueContext getTaskQueueContext()
    {
        return taskQueueContext;
    }

    public Injector<HashIndexManager<Bytes2LongHashIndex>> getTaskTypeIndexServiceInjector()
    {
        return taskTypeIndexServiceInjector;
    }

    public Injector<DeferredResponsePool> getResponsePoolServiceInjector()
    {
        return responsePoolServiceInjector;
    }

}
