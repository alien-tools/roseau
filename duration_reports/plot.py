import matplotlib.pyplot as plt
import csv
import numpy as np

# Read execution times from the benchmark_libraries CSV file
tasks_benchmark_libraries = []
execution_times_benchmark_libraries = []

with open('benchmark_libraries.csv', 'r') as csvfile:
    csvreader = csv.reader(csvfile)
    next(csvreader)  
    for row in csvreader:
        tasks_benchmark_libraries.append(row[0])
        execution_times_benchmark_libraries.append(float(row[1]))

# Read execution times from the spoon CSV file
tasks_spoon = []
execution_times_spoon = []

with open('spoon.csv', 'r') as csvfile:
    csvreader = csv.reader(csvfile)
    next(csvreader)  
    for row in csvreader:
        tasks_spoon.append(row[0])
        execution_times_spoon.append(float(row[1]))

# Read execution times from the revapi CSV file
tasks_revapi = []
execution_times_revapi = []

with open('revapi_library.csv', 'r') as csvfile:
    csvreader = csv.reader(csvfile)
    next(csvreader)  
    for row in csvreader:
        tasks_revapi.append(row[0])
        execution_times_revapi.append(float(row[1]))



# Prepare data for the bar chart
x = ['benchmark_libraries', 'revapi_library', 'spoon_library']
spoon_model_execution_times = [
    execution_times_benchmark_libraries[0],
    execution_times_revapi[0],
    execution_times_spoon[0]
]


API_extractions_execution_times  = [
    execution_times_benchmark_libraries[1],
    execution_times_revapi[1],
    execution_times_spoon[1]
]

delta_execution_times = [
    execution_times_benchmark_libraries[2],
    execution_times_revapi[2],
    execution_times_spoon[2]
]




# Create stacked bar chart
plt.bar(x, spoon_model_execution_times, label=tasks_benchmark_libraries[0], color='g')
plt.bar(x, API_extractions_execution_times, bottom=spoon_model_execution_times, label=tasks_benchmark_libraries[1], color='y')
plt.bar(x, delta_execution_times, bottom=[sum(x) for x in zip(spoon_model_execution_times, API_extractions_execution_times)],
        label=tasks_benchmark_libraries[2], color='orange')

# Add labels and legend
plt.xlabel('Libraries')
plt.ylabel('Execution Times (ms)')
plt.title('Execution times of each phase by libraries')
plt.legend()


# Saving it as a png
plt.tight_layout()
plt.savefig('executions_times_for_libraries.png')  

