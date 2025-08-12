import subprocess # to run pip install commands
import sys
import re
import os
import json
import requests

def check_virtual_environment():
    """Check if a virtual environment is activated."""
    venv =  hasattr(sys, 'real_prefix') or (hasattr(sys, 'base_prefix') and sys.base_prefix != sys.prefix)
    if not venv:
        print("❌ Error: This script must be run inside a virtual environment.")
        print("Please activate your virtual environment and try again.")
        sys.exit(1)

def find_packages(target_script_path:str):
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
        return set()

    full_output = result.stdout + result.stderr

    packages_to_install = set()
    for line in full_output.splitlines():
        match = re.search(r"use pip install\s+([a-zA-Z0-9._-]+)", line)
        if match:
            package = match.group(1)
            print(f"🔍 Found package recommendation: {package}")
            packages_to_install.add(package)

    return packages_to_install

def install_packages(packages_to_install: set):
    if not packages_to_install:
        print("No packages to install.")
        return

    print(f"📦 Missing packages to install: {', '.join(sorted(list(packages_to_install)))}")
    for package in sorted(list(packages_to_install)):
        print(f"Attempting to install package: {package}")
        install_command = [sys.executable, "-m", "pip", "install", package]
        
        print(f"Running command: {' '.join(install_command)}")
        install_result = subprocess.run(
            install_command,
            capture_output=True,
            text=True,
            encoding='utf-8',
            check=False
        )

        if install_result.returncode == 0:
            print(f"✅ Successfully installed {package}")
        else:
            print(f"❌ Failed to install {package}:\n{install_result.stderr.strip()}")
            print(f"  Full pip output:\n{install_result.stdout.strip()}")

def format_bytes(size):
    """Formats bytes into human-readable units."""
    for unit in ['bytes', 'KB', 'MB', 'GB', 'TB']:
        if size < 1024.0:
            return f"{size:.2f} {unit}"
        size /= 1024.0

def get_package_download_size_from_pypi(package_name, version=None):
    pypi_url = f"https://pypi.org/pypi/{package_name}/json"
    
    try:
        response = requests.get(pypi_url, timeout=5)
        response.raise_for_status()
        data = response.json()
    except requests.exceptions.Timeout:
        return None
    except requests.exceptions.ConnectionError:
        return None
    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 404:
            pass
        else:
            print(f"  Warning: Error fetching data for '{package_name}' from PyPI: {e}")
        return None
    except json.JSONDecodeError:
        print(f"  Warning: Could not decode JSON response from PyPI for '{package_name}'.")
        return None
    
    target_version_info = None
    if version:
        if version in data['releases']:
            target_version_info = data['releases'][version]
        else:
            return None
    else:
        latest_version = data['info']['version']
        target_version_info = data['releases'].get(latest_version)
        if not target_version_info:
            return None

    wheel_size = None
    sdist_size = None
    
    for file_info in target_version_info:
        file_type = file_info.get('packagetype')
        size = file_info.get('size')
        
        if size is None:
            continue

        if file_type == 'bdist_wheel':
            if wheel_size is None or size > wheel_size:
                wheel_size = size
        elif file_type == 'sdist':
            if sdist_size is None or size > sdist_size:
                sdist_size = size
    
    if wheel_size is not None:
        return wheel_size
    elif sdist_size is not None:
        return sdist_size
    
    return None

def calculate_total_download_size(packages_to_check: set):
    print("\n--- Estimating total download size for missing packages ---")
    total_size_bytes = 0
    package_details = []
    
    for package in sorted(list(packages_to_check)):
        size = get_package_download_size_from_pypi(package)
        if size is not None:
            total_size_bytes += size
            package_details.append(f"  - {package}: {format_bytes(size)}")
        else:
            package_details.append(f"  - {package}: Size unknown (could not retrieve from PyPI)")
            
    for detail in package_details:
        print(detail)

    print(f"\nTotal estimated download size for {len(packages_to_check)} packages: {format_bytes(total_size_bytes)}")
    return total_size_bytes

def run_and_install(target_script_path: str, threshold_bytes: int, ml_dir_flag: bool):
    check_virtual_environment()

    if not os.path.exists(target_script_path):
        print(f"❌ Error: Target script '{target_script_path}' not found.")
        sys.exit(1)

    print(f"\n--- Starting automated dependency resolution for: {target_script_path} ---")

    attempt_count = 0
    max_attempts = 3

    while attempt_count < max_attempts:
        attempt_count += 1
        print(f"\nAttempt {attempt_count} of {max_attempts}...")
        
        packages_to_install = find_packages(target_script_path)

        if not packages_to_install:
            print("✅ No additional packages needed. Script executed successfully.")
            sys.exit(0)

        total_estimated_size = calculate_total_download_size(packages_to_install)
        print(f"Total estimated download size: {format_bytes(total_estimated_size)}")
        if total_estimated_size > threshold_bytes and total_estimated_size >= 0:
            user_choice = input("This installation is large. Continue? (y/n): ").lower()
            if user_choice != 'y':
                print("Installation cancelled by user.")
                sys.exit(0)

        install_packages(packages_to_install)

    if attempt_count == max_attempts:
        print("❗ Maximum attempts reached. Proceed with manual package installation.")
        sys.exit(1)

def get_threshold(arguments):
    threshold_mb = 50

    i = 0
    while i < len(arguments):
        arg = arguments[i]

        if arg == '-t':
            if i + 1 < len(arguments):
                threshold_mb = int(arguments[i + 1])
            else:
                print("Error: Please supply threshold")

            if threshold_mb < 0:
                print("Error: Threshold value must pe an integer")

        i = i + 1

    return threshold_mb * 1024 * 1024

def main():
    if len(sys.argv) < 2:
        print("Usage: python auto_python.py <python_program_path>")
        sys.exit(1)

    target_script_path = sys.argv[1]

    threshold_bytes = get_threshold(sys.argv)

    ml_dir_flag = False
    if "--ml-dir" in sys.argv:
        ml_dir_flag = True

    run_and_install(target_script_path, threshold_bytes, ml_dir_flag)

if __name__ == "__main__":
    main()

# argumente : primeste o functie si un threshold de dimensiune totala pachete (pentru confirmare)
# arguement: flag instalare in ml-directory/nu 
# un numar >= 0 -> ver total + conf, negativ nu cere

# ver daca este in venv
# tine minte locatia pentru activare ulterioara
# activeaza venv din ml-directory (daca flag)
# daca nu ramane unde este

# daca a activat din ml-directory, de fiecare data dupa ce instaleaza merge inapoi in venv initial (daca era)
# daca nu era, dezactiveava venv din ml-directory

# transform in pachet, dar si posibil cu main
# interactiune cu utilizatorul

# auto_python.py <script_path> -flag threshold
# modul: auto_python (script_path, flag, threshold)