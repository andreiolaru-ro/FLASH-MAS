
from importlib import import_module
from sys import stderr
from typing import List, Tuple, Dict

head = "<Fed py> "
def log(*args): print(f"{head}", *args)

def logE(*args): print(f"{head}", *args, file = stderr, flush = True)


def import_functionality(name, pippackage = None, critical = False, autoinstall = False):
    components = name.split('.')
    package = components[0]
    log("importing", package)
    try:
        mod = import_module(package)
        for comp in components[1:]:
            mod = getattr(mod, comp)
        return mod
    except Exception as e:
        pippackage = pippackage if pippackage is not None else name.split(".")[0]
        log(package, "unavailable (use pip install", pippackage, "):", e)
        # TODO check if should autoinstall, and then do
        if critical: exit(1)
    return None

Metrics = Dict[str, float]
def weighted_average(metrics: List[Tuple[int, Metrics]]) -> Metrics:
    # Multiply accuracy of each client by number of examples used
    accuracies = [num_examples * m["accuracy"] for num_examples, m in metrics]
    examples = [num_examples for num_examples, _ in metrics]

    # Aggregate and return custom metric (weighted average)
    return {"accuracy": sum(accuracies) / sum(examples)}

Metrics = Dict[str, float]

def weighted_average_fit_metrics(metrics: List[Tuple[int, Metrics]]) -> Metrics:
    """Compute weighted average of fit metrics (e.g., accuracy)."""
    if not metrics:
        return {}

    total_examples = sum(num_examples for num_examples, _ in metrics)
    aggregated = {}

    for key in metrics[0][1].keys():
        weighted_sum = sum(num_examples * m[key] for num_examples, m in metrics)
        aggregated[key] = weighted_sum / total_examples

    return aggregated
