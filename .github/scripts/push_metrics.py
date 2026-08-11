import os
import sys
import time

from prometheus_remote_writer import RemoteWriter

writer = RemoteWriter(
    url=os.environ["GRAFANA_PROM_URL"],
    auth={
        "username": os.environ["GRAFANA_PROM_USER"],
        "password": os.environ["GRAFANA_TOKEN"],
    },
)

now = int(time.time() * 1000)
metrics = []

with open(sys.argv[1]) as handle:
    for line in handle:
        parts = line.split()
        if not parts:
            continue
        service, up, warm, seconds = parts
        for name, value in (
            ("tp243_up", int(up)),
            ("tp243_warm", int(warm)),
            ("tp243_wake_seconds", int(seconds)),
        ):
            metrics.append(
                {
                    "metric": {"__name__": name, "service": service},
                    "values": [value],
                    "timestamps": [now],
                }
            )

result = writer.send(metrics)
print(f"Sent {len(metrics)} samples: {result}")
