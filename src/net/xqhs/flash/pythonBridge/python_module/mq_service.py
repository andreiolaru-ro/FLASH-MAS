"""
Python side of PythonMqBridgeDriver: FastAPI accepts jobs, a worker pool runs
them, and results are published to a Redis stream.

The point of this transport is that submitting and answering are decoupled:
/infer returns as soon as the job is accepted, and the result shows up later on
`results:<agent_id>`, which the Java side is listening to. That is what lets
PythonBridgeDriver.processAsync() return immediately without parking a thread.

Requires a reachable Redis server:
`docker compose up -d` or a local `redis-server`.
"""

import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from util import import_functionality, log

import_functionality("fastapi", critical=True)
import_functionality("uvicorn", critical=True)
import_functionality("pydantic", critical=True)
redis = import_functionality("redis", critical=True)

from concurrent.futures import ThreadPoolExecutor
from typing import Any, Dict

import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel

SERVICE_PORT = 8000
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
# deliberately short: long enough to prove the result really arrives out of band,
# short enough not to slow the tests down.
WORK_SECONDS = 2

app = FastAPI()
executor = ThreadPoolExecutor(max_workers=4)
r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)


class InferenceRequest(BaseModel):
    agent_id: str
    job_id: str
    payload: Dict[str, Any]


@app.get("/health")
def health():
    # polled by the Java side to know the service has finished starting
    return {"status": "ok"}


@app.post("/infer")
def infer(req: InferenceRequest):
    executor.submit(_run, req.agent_id, req.job_id, req.payload)
    return {"status": "accepted", "job_id": req.job_id}


def _run(agent_id: str, job_id: str, payload: dict):
    result = run_inference(payload)
    publish_result(agent_id, job_id, result)


def run_inference(payload):
    log("working on:", payload)
    time.sleep(WORK_SECONDS)
    return {"prediction": 42, "echo": str(payload)}


def publish_result(agent_id: str, job_id: str, result: dict):
    stream = f"results:{agent_id}"
    r.xadd(stream, {"job_id": job_id, "result": str(result)})
    log("published result for job", job_id, "on", stream)


if __name__ == '__main__':
    log("all required packages available, starting service on port", SERVICE_PORT)
    uvicorn.run(app, host="0.0.0.0", port=SERVICE_PORT, log_level="warning")