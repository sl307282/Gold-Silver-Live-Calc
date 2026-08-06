import os
import glob

directory = 'app/src/main/java/com/goldsilver/livecalc'
kt_files = glob.glob(f'{directory}/**/*.kt', recursive=True)

import_statement = "import androidx.lifecycle.compose.collectAsStateWithLifecycle\n"

for file_path in kt_files:
    with open(file_path, 'r') as f:
        content = f.read()
    
    if '.collectAsState()' in content:
        # Check if the import already exists
        if 'import androidx.lifecycle.compose.collectAsStateWithLifecycle' not in content:
            # Find the last import statement and append ours after it
            lines = content.split('\n')
            last_import_idx = -1
            for i, line in enumerate(lines):
                if line.startswith('import '):
                    last_import_idx = i
            
            if last_import_idx != -1:
                lines.insert(last_import_idx + 1, "import androidx.lifecycle.compose.collectAsStateWithLifecycle")
                content = '\n'.join(lines)
            
        content = content.replace('.collectAsState()', '.collectAsStateWithLifecycle()')
        
        with open(file_path, 'w') as f:
            f.write(content)
        print(f"Updated {file_path}")
