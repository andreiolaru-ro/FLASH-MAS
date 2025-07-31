# auto_python.py

import subprocess # to run pip install commands
import sys 
import re # to handle regex for package names
import os # to handle file paths
import time # to handle time delays

def check_virtual_environment():
    """Check if a virtual environment is activated."""
    venv =  hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix)
    if not venv:
        print("❌ Error: This script must be run inside a virtual environment.")
        print("Please activate your virtual environment and try again.")
        sys.exit(1)

def run_and_install(target_script_path: str):
    if not os.path.exists(target_script_path):
        print(f"❌ Error: Target script '{target_script_path}' not found.")
        sys.exit(1)

    print(f"\n--- Starting automated dependency resolution for: {target_script_path} ---")

    attempt_count = 0
    max_attempts = 3

    while attempt_count < max_attempts:
        attempt_count += 1
        print(f"\nAttempt {attempt_count} of {max_attempts}...")
        print(f"Running: python {target_script_path}")

        try:
            result = subprocess.run(
                [sys.executable, target_script_path], 
                capture_output=True,
                text=True,
                encoding='utf-8',
                check=False
                )
        except Exception as e:
            print(f"❌ Error running script: {e}")
        
        full_output = result.stdout + result.stderr

        packages_to_install = set()
        for line in full_output.splitlines():
            if "use pip install" in line:
                match = re.search(r"use pip install\s+([^\s,;]+)", line)
                if match:
                    package = match.group(1)
                else:
                    continue
                print(f"🔍 Found package recommendation: {package}")
                packages_to_install.add(package)

        if not packages_to_install:
            print("✅ No additional packages needed. Script executed successfully.")
            break

        # packages_to_install.sort()
        print(f"📦 Missing packages: {', '.join(packages_to_install)}")
        for package in packages_to_install:
            print(f"Installing package: {package}")
            install_result = subprocess.run(
                [sys.executable, "-m", "pip", "install", package],
                capture_output=True,
                text=True,
                encoding='utf-8',
                check=False
            )
            print(f"Running command: {sys.executable} -m pip install {package}")
            if install_result.returncode == 0:
                print(f"✅ Successfully installed {package}")
            else:
                print(f"❌ Failed to install {package}: {install_result.stderr.strip()}")

    if attempt_count == max_attempts:
        print("❗ Maximum attempts reached. Proceed with manual package instalation.")
        sys.exit(1)

def main():
    if len(sys.argv) < 2:
        print("Usage: python auto_python.py <python_program_path>")
        sys.exit(1)

    check_virtual_environment()

    target_script_path = sys.argv[1]
    run_and_install(target_script_path)

if __name__ == "__main__":
    main()