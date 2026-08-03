import os
import sys

def remove_bom_from_file(filepath):
    try:
        with open(filepath, 'rb') as f:
            content = f.read()
        
        # Check if BOM exists (EF BB BF)
        if content.startswith(b'\xef\xbb\xbf'):
            # Remove BOM
            content = content[3:]
            with open(filepath, 'wb') as f:
                f.write(content)
            print(f'✅ Fixed: {os.path.basename(filepath)}')
            return True
        return False
    except Exception as e:
        print(f'❌ Error: {filepath} - {e}')
        return False

def main():
    root_dir = 'src/main/java'
    fixed_count = 0
    
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if remove_bom_from_file(filepath):
                    fixed_count += 1
    
    print(f'\n✅ Fixed {fixed_count} files!')
    print('Now run: mvn clean compile')

if __name__ == '__main__':
    main()
