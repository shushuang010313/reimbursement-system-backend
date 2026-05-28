import os
import re

def refactor_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find all fully qualified class names that are NOT part of an import or package statement
    # We use a regex that matches word boundaries.
    # We'll split the file into lines to make it easier to skip import/package lines,
    # or just use a lookbehind for import/package.
    
    # regex for finding fully qualified names like java.util.Map or com.shengyi...ReimSplitDTO
    fqn_pattern = re.compile(r'\b([a-z0-9_]+(?:\.[a-z0-9_]+)+\.([A-Z][a-zA-Z0-9_]*))\b')
    
    lines = content.split('\n')
    new_lines = []
    imports_to_add = set()
    
    changed = False
    
    for line in lines:
        if line.strip().startswith('import ') or line.strip().startswith('package '):
            new_lines.append(line)
            continue
            
        # Find all matches in this line
        matches = fqn_pattern.findall(line)
        if matches:
            for match in matches:
                full_name = match[0]
                class_name = match[1]
                
                # Check if it's a valid class name to import
                if full_name.startswith('java.lang.'):
                    # java.lang classes don't need imports, just replace them
                    line = line.replace(full_name, class_name)
                    changed = True
                else:
                    # We need to add an import
                    imports_to_add.add(full_name)
                    line = line.replace(full_name, class_name)
                    changed = True
        new_lines.append(line)
        
    if not changed:
        return False
        
    # Now we need to insert the new imports.
    # Find the last import statement or the package statement
    insert_idx = 0
    for i, line in enumerate(new_lines):
        if line.strip().startswith('import '):
            insert_idx = i + 1
        elif insert_idx == 0 and line.strip().startswith('package '):
            insert_idx = i + 1
            
    # Check what imports are already there
    existing_imports = set()
    for line in new_lines:
        if line.strip().startswith('import '):
            # extract the imported class
            m = re.match(r'import\s+([^;]+);', line.strip())
            if m:
                existing_imports.add(m.group(1))
                
    actual_imports_to_add = [imp for imp in imports_to_add if imp not in existing_imports]
    
    if actual_imports_to_add:
        # Create import statements
        import_statements = [f"import {imp};" for imp in sorted(actual_imports_to_add)]
        
        # Insert them
        if insert_idx > 0 and not new_lines[insert_idx-1].strip():
             # insert after empty line if there is one
             pass
        elif insert_idx > 0:
             # insert an empty line before our imports if we are just after package
             if new_lines[insert_idx-1].strip().startswith('package '):
                 import_statements.insert(0, "")
                 
        new_lines[insert_idx:insert_idx] = import_statements
        
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write('\n'.join(new_lines))
        
    print(f"Refactored: {filepath}")
    return True

def main():
    src_dir = r"E:\code\reimbursement-system\src"
    count = 0
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                if refactor_file(filepath):
                    count += 1
    print(f"Total files refactored: {count}")

if __name__ == "__main__":
    main()
