## Accuracy evaluation

We evaluate the accuracy of Roseau using multiple API evolution datasets from various sources.

Each dataset consists of a set of cases. Each case includes: a baseline API (`v1`), an updated version of that API (`v2`), and a `main()` method that uses symbols from the baseline API (`client`).
Each case is validated by systematically compiling the baseline API, the updated API, and the client against the baseline API.

The client is then recompiled and relinked against the updated API.
If the compiler reports an error, the case is marked as _source-incompatible_. If the linker reports an error, the case is marked as _binary-incompatible_.

Each tool is given the two API versions and must identify source- or binary-breaking changes. Tool results are compared against the ground truth to compute accuracy metrics.
The tools do not have access to the clients.

### Full benchmark and execution instructions

- [https://github.com/tdegueul/roseau-full-bench](https://github.com/tdegueul/roseau-full-bench)

### Evaluated tools

- [Roseau](https://github.com/alien-tools/roseau) (`v0.7.0-SNAPSHOT`)
- [japicmp](https://siom79.github.io/japicmp/) (`v0.25.4`)
- [Revapi](https://revapi.org) (`v0.28.4`)

### Datasets

- Jezek (310 cases): presented in [API Evolution and Compatibility: A Data Corpus and Tool Evaluation](https://www.jot.fm/issues/issue_2017_04/article2.pdf) by Jezek and Dietrich. We manually fixed some buggy cases and significantly strengthened the clients to address false negatives.
- Roseau (422 cases): the cases are automatically extracted from [Roseau's test suite](https://github.com/alien-tools/roseau/tree/main/core/src/test/java/io/github/alien/roseau/diff)

### Design choices

- The benchmark uses the Java 25 compiler and linker (`OpenJDK Runtime Environment (build 25.0.3)`).
- The benchmark strictly focuses on source- and binary-breaking changes; behavioral/semantic breaking changes are not considered.
- When client code breaks, it is guaranteed that the case is indeed breaking, as stated by the compiler and/or linker. When client code doesn't break, however, it is possible that this particular client doesn't break, but that a different one using the baseline API differently would. We do our best to write exhaustive clients that fully exercise the baseline API in all possible ways.
- The baseline API and client code are located in different Java packages. Therefore, the benchmark assumes that package-private symbols are not part of the API.
- The benchmark evaluates whether the tools identify *some* breaking changes with the right compatibility level (source or binary). However, it does not evaluate whether the breaking change kind reported by the tools (e.g., `CLASS_NOW_FINAL`) indeed corresponds to the case.

### Results

<table>
  <thead>
    <tr>
      <th>Dataset</th>
      <th>Category</th>
      <th>Metric</th>
      <th>Roseau</th>
      <th>japicmp</th>
      <th>Revapi</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td rowspan="9">Jezek</td>
      <td rowspan="3">Breaking</td>
      <td>Precision</td>
      <td><strong>0.98</strong></td>
      <td>0.89</td>
      <td>0.82</td>
    </tr>
    <tr>
      <td>Recall</td>
      <td><strong>1.00</strong></td>
      <td>0.83</td>
      <td>0.97</td>
    </tr>
    <tr>
      <td>F1</td>
      <td><strong>0.99</strong></td>
      <td>0.86</td>
      <td>0.89</td>
    </tr>
    <tr>
      <td rowspan="3">Source</td>
      <td>Precision</td>
      <td><strong>0.90</strong></td>
      <td>0.78</td>
      <td>0.74</td>
    </tr>
    <tr>
      <td>Recall</td>
      <td><strong>1.00</strong></td>
      <td>0.82</td>
      <td>0.96</td>
    </tr>
    <tr>
      <td>F1</td>
      <td><strong>0.95</strong></td>
      <td>0.80</td>
      <td>0.84</td>
    </tr>
    <tr>
      <td rowspan="3">Binary</td>
      <td>Precision</td>
      <td><strong>0.95</strong></td>
      <td>0.91</td>
      <td>0.92</td>
    </tr>
    <tr>
      <td>Recall</td>
      <td><strong>1.00</strong></td>
      <td><strong>1.00</strong></td>
      <td>0.97</td>
    </tr>
    <tr>
      <td>F1</td>
      <td><strong>0.98</strong></td>
      <td>0.95</td>
      <td>0.94</td>
    </tr>
    <tr>
      <td rowspan="9">Roseau</td>
      <td rowspan="3">Breaking</td>
      <td>Precision</td>
      <td><strong>0.98</strong></td>
      <td>0.70</td>
      <td>0.75</td>
    </tr>
    <tr>
      <td>Recall</td>
      <td><strong>0.99</strong></td>
      <td>0.85</td>
      <td>0.91</td>
    </tr>
    <tr>
      <td>F1</td>
      <td><strong>0.98</strong></td>
      <td>0.76</td>
      <td>0.82</td>
    </tr>
    <tr>
      <td rowspan="3">Source</td>
      <td>Precision</td>
      <td><strong>0.98</strong></td>
      <td>0.65</td>
      <td>0.71</td>
    </tr>
    <tr>
      <td>Recall</td>
      <td><strong>0.99</strong></td>
      <td>0.84</td>
      <td>0.88</td>
    </tr>
    <tr>
      <td>F1</td>
      <td><strong>0.98</strong></td>
      <td>0.73</td>
      <td>0.79</td>
    </tr>
    <tr>
      <td rowspan="3">Binary</td>
      <td>Precision</td>
      <td><strong>0.84</strong></td>
      <td>0.76</td>
      <td>0.70</td>
    </tr>
    <tr>
      <td>Recall</td>
      <td><strong>1.00</strong></td>
      <td>0.98</td>
      <td>0.95</td>
    </tr>
    <tr>
      <td>F1</td>
      <td><strong>0.91</strong></td>
      <td>0.85</td>
      <td>0.81</td>
    </tr>
  </tbody>
</table>
