package edu.berkeley.myberkeley.notifications.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;

import java.util.Dictionary;
import java.util.Hashtable;

public class QueuedNotificationSenderTest extends Assert {

    private QueuedNotificationSender sender;

    private QueuedNotificationSender.SendNotificationsJob job;

    @Before
    public void setup() {
        this.sender = new QueuedNotificationSender();
        this.sender.repository = mock(SlingRepository.class);
        this.sender.scheduler = mock(Scheduler.class);
        this.job = this.sender.getJob();
    }

    @Test
    public void activate() throws Exception {
        ComponentContext context = mock(ComponentContext.class);
        Dictionary<String, Long> dictionary = new Hashtable<String, Long>();
        dictionary.put(QueuedNotificationSender.PROP_POLL_INTERVAL_SECONDS, 60L);
        when(context.getProperties()).thenReturn(dictionary);
        this.sender.activate(context);
    }

    @Test
    public void deactivate() throws Exception {
        ComponentContext context = mock(ComponentContext.class);
        this.sender.deactivate(context);
    }

    @Test
    public void execute() {
        JobContext context = mock(JobContext.class);
        this.job.execute(context);
    }
}
