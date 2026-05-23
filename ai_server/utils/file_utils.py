import os


def file_exists(path: str) -> bool:
    return os.path.isfile(path)


def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)
