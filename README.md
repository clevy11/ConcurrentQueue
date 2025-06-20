# ConcurQueue - A Multithreaded Job Processing Platform

ConcurQueue is a high-performance, concurrent job processing system designed to efficiently handle multiple producers submitting jobs and distribute them to worker threads for processing.

## Features

- **Concurrent Processing**: Multiple worker threads process tasks in parallel
- **Priority Queue**: Tasks are processed based on their priority (higher priority first)
- **Task Retry**: Failed tasks are automatically retried (configurable number of retries)
- **Monitoring**: Built-in monitoring of queue status and task processing
- **Graceful Shutdown**: Proper cleanup of resources on shutdown
- **Thread Safety**: Thread-safe implementation using Java's concurrent utilities

## Requirements

- Java 17 or higher
- Maven 3.8.1 or higher

## Building the Project

```bash
mvn clean package
```

## Running the Application

```bash
mvn exec:java -Dexec.mainClass="com.example.clb.Main"
```

## Project Structure

```
src/main/java/com/example/clb/
├── Main.java                 # Main application class
├── model/
│   ├── Task.java           # Task model with priority and retry logic
│   └── TaskStatus.java      # Enum for task statuses (SUBMITTED, PROCESSING, etc.)
├── producer/
│   └── TaskProducer.java    # Simulates task producers
└── service/
    └── TaskDispatcher.java  # Manages task queue and worker threads
```

## Configuration

You can modify the following constants in `Main.java` to customize the behavior:

- `NUM_PRODUCERS`: Number of producer threads (default: 3)
- `TASKS_PER_PRODUCER`: Number of tasks each producer will create (default: 15)
- `NUM_WORKERS`: Number of worker threads (default: 5)
- `QUEUE_CAPACITY`: Maximum number of tasks that can be queued (default: 50)
- `MAX_RETRIES`: Maximum number of retries for failed tasks (default: 3)

## Monitoring

The application includes a built-in monitor that logs the following information every 5 seconds:

- Queue size
- Number of tasks processed
- Number of active threads
- Number of pending tasks
- Task status counts (SUBMITTED, PROCESSING, COMPLETED, FAILED)

## Shutdown

The application can be shut down by pressing Ctrl+C. It will:

1. Stop accepting new tasks
2. Allow in-progress tasks to complete
3. Shut down worker threads gracefully
4. Print a summary before exiting

## Testing

To run the tests:

```bash
mvn test
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
