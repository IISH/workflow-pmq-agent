# Message queue agent

This is an agent to listen to ActiveMQ server messages.

The library is part of the workflow controller. The main documentation you can find at:

[Workflow controller on github](https://github.com/IISH/workflow-controller)

## Write tasks

Tasks are simple bash files in a queue folder.

For example:

    queues
    ├── hello_world_1
    │   └── startup.sh
    ├── hello_world_2
    │   └── startup.sh

Notice the identical name between the folder name and the queue name in the task element.

###How to set the queues for an agent
Place symbolic links in the queues_enabled folder to the folders in the queues folder.

###Types of messages
The agent will listen to the message queue for:

message: only one agent will pick up the message and run the task.

topic: all agents will pick up the message and run the task

Example:

In the message folder you see all queues the agent in listening for messages. But the topic folder is empty, so it will ignore topics.

    queues_enabled
    ├── message
    │   ├── hello_world_10 -> ../../queues/hello_world_10
    │   ├── hello_world_20 -> ../../queues/hello_world_20
    ├── settings.sh -> ../queues/settings.sh
    └── topic
        └── README.txt
