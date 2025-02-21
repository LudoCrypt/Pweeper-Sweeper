import os

def save_png_list():
    folder_path = os.path.join(os.getcwd(), "src", "resources")
    output_file = os.path.join(folder_path, "sprites.lst")
    
    png_list = [f for f in os.listdir(folder_path) if f.endswith(".png")]
    
    with open(output_file, "w") as file:
        file.write("\n".join(png_list))

save_png_list()
