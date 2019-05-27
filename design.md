live log - connection
--------
- log lines from 1-10 are in the storage and new lines are coming
- user subscribes to WS lastResult updates
    - user receives lines from 5 to 10
    - user receives a message for each new lastResult

Backend
- user subscription received
- starts a pooling thread to monitor for new logs
    - monitor only for users criteria (can be latter on optimized)
- node crashes:
    - user have to re-connect -> re-start pooling timer


live log - WebSocket push blocking vs. async in case of slow subscribed client
--------
blocking
- same thread reading ES stram and pushing the messages to the client
- threads are blocked on IO operations
    - ES fetch is blocked by the client
- no buffer filling
- no unnecessary fetching from ES

async
- messages read from ES pushed to buffer and waiting for consumer
- no threads are blocked
- messages start filling a buffer
- problem can be limited with timeout
    - there will still be unnecessary fetching from ES and filling the buffer
- needs flow control
    - keep the number of the message in the buffer
    - source fetching loop, checks is a consumer is ready, if not source ands its next fetch task to the scheduler


stored log - get
--------
WS request get(filter, from/after, size, direction=DESC)
- note that WS "action get"(request) processing should not wait for the response processing. Query source in a new thread.
- similar situation for blocking vs async

