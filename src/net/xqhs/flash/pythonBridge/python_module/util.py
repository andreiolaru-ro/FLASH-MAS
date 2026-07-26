from importlib import import_module
from sys import stderr, exit

head = "<bridge server> "


def log(*args):
    print(f"{head}", *args, flush=True)


def logE(*args):
    print(f"{head}", *args, file=stderr, flush=True)


def import_functionality(name, pippackage=None, critical=False):
    """
    Tries to import `name` (dotted path allowed, e.g. "some.thing").
    On success returns the resolved module/attribute.
    On failure, logs a line the Java side can parse to find out what's
    missing, and if `critical` is True, exits the process immediately
    (so the caller can detect the failed attempt quickly instead of
    waiting for a timeout).
    """
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
        if critical:
            exit(1)
    return None