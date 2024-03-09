# GPT-4 generated, untouched

import csv
import sys
from pygments import highlight
from pygments.lexers import JavaLexer
from pygments.formatters import HtmlFormatter

# Check for command-line argument
if len(sys.argv) < 2:
    print("Usage: script.py <path_to_csv_file>")
    sys.exit(1)

csv_file_path = sys.argv[1]

# Function to extract and highlight code with specific line marked
def extract_and_highlight(file_path, line_num, context=3):
    if not file_path:
        return "Position not specified."
    try:
        with open(file_path, 'r') as file:
            lines = file.readlines()
            start_line = max(line_num - context, 0)
            end_line = line_num + context
            highlighted_code = highlight(''.join(lines[start_line:end_line]), JavaLexer(), HtmlFormatter())
            return highlighted_code
    except FileNotFoundError:
        return "File not found."

# Main function to parse the CSV, sort, and generate the HTML
def generate_html(csv_path):
    breaking_changes = []

    with open(csv_path, newline='') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            element = row['element']
            old_position = row.get('oldPosition', '')  # Corrected column name
            new_position = row.get('newPosition', '')
            kind = row['kind']
            nature = row['nature']

            old_file_path, old_line_num_str = old_position.split(':') if old_position else (None, '0')
            new_file_path, new_line_num_str = new_position.split(':') if new_position else (None, '0')
            old_line_num = int(old_line_num_str)
            new_line_num = int(new_line_num_str)

            old_code_highlighted = extract_and_highlight(old_file_path, old_line_num) if old_position else 'Not applicable.'
            new_code_highlighted = extract_and_highlight(new_file_path, new_line_num) if new_position else 'Not applicable.'

            breaking_changes.append({
                'element': element,
                'old_code_highlighted': old_code_highlighted,
                'new_code_highlighted': new_code_highlighted,
                'kind': kind,
                'nature': nature
            })

    # Sort breaking changes by the kind
    breaking_changes.sort(key=lambda x: x['kind'])

    # HTML content with Bootstrap for wider layout
    html_content = '''
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Breaking Changes Report</title>
    <link href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" rel="stylesheet">
    <style>
        ''' + HtmlFormatter().get_style_defs('.highlight') + '''
        mark { background-color: #ffff99; color: black; }
        .container { max-width: 95%; }
        .card { margin-bottom: 20px; }
    </style>
</head>
<body>
    <div class="container mt-5">
        <h1>Breaking Changes Report</h1>
'''

    for change in breaking_changes:
        html_content += f'''
        <div class="card">
            <div class="card-header">
                {change['element']} - {change['kind']}
            </div>
            <div class="card-body">
                <div class="row">
                    <div class="col-md-6">
                        <h5>Old Version</h5>
                        <div class="highlight">{change['old_code_highlighted']}</div>
                    </div>
                    <div class="col-md-6">
                        <h5>New Version</h5>
                        <div class="highlight">{change['new_code_highlighted']}</div>
                    </div>
                </div>
            </div>
        </div>
'''

    html_content += '''
    </div>
    <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.5.2/dist/umd/popper.min.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
</body>
</html>
'''

    # Save the HTML file
    with open('breaking_changes_report.html', 'w') as html_file:
        html_file.write(html_content)

generate_html(csv_file_path)

