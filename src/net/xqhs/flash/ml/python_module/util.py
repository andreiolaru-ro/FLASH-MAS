from importlib import import_module
from sys import stderr

head = "<ML server> "
def log(*args): print(f"{head}", *args)
def logE(*args): print(f"{head} error>", *args, file=stderr, flush=True)

def import_functionality(name, pippackage=None, critical=False, autoinstall=False, expect_cls=True):
    components = name.split('.')
    package = components[0]
    log("importing", package)

    try:
        mod = import_module(package)
        for comp in components[1:]:
            mod = getattr(mod, comp)

        if expect_cls:
            # Expect a `cls` attribute
            if hasattr(mod, "__file__"):  # module
                if hasattr(mod, "cls"):
                    return getattr(mod, "cls")
                else:
                    raise ImportError(f"Module '{name}' does not define 'cls'")
            else:
                return mod
        else:
            return mod

    except Exception as e:
        pippackage = pippackage if pippackage else name.split(".")[0]
        logE(f"{package} unavailable (use pip install {pippackage}): {e}")
        if critical:
            exit(1)
        return None
