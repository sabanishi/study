import os

def count_empty_files_in_folder(folder_path, file_name="n-diff.txt"):
    """
    Counts the number of empty files with a specified name in a folder.

    Args:
        folder_path (str): Path to the folder to scan.
        file_name (str): Name of the files to check (default is "n-diff.txt").

    Returns:
        int: Count of empty files.
    """
    empty_file_count = 0

    # Check if the folder exists
    if not os.path.isdir(folder_path):
        print(f"The folder '{folder_path}' does not exist.")
        return 0

    # Iterate over all files in the folder
    for root, _, files in os.walk(folder_path):
        for file in files:
            file_path = os.path.join(root, file)
            # Check if the file is empty
            if os.path.isfile(file_path) and os.path.getsize(file_path) == 0:
                empty_file_count += 1

    return empty_file_count

# Example usage
folder_path = "match"  # Replace with the actual folder path
empty_file_count = count_empty_files_in_folder(folder_path)
print(f"Number of empty 'n-diff.txt' files: {empty_file_count}")
