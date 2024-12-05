
from importlib import import_module
from sys import stderr

head = "<ML server> "
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

