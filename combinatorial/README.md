# Combinatorial Benchmark

## Introduction

This benchmark is designed to evaluate the performance of tools detecting breaking changes.
It is divided into three parts:
- **API v1 generation**: Generates an API with all (*kind of*) possible combinations of types and type members uses.
- **API client generation**: Visits the API v1 and generates one client interacting with it.
- **API v2 generations and evaluation**: Visits the API v1, generates a new API v2 for each atomic change possible, computes the ground truth using the client against the new API v2 then evaluates the breaking changes detection tools.

## API v1 generation

We generate the following types for the API v1:
- **[Classes](#classes)**
- **[Interfaces](#interfaces)**
- **[Enums](#enums)**
- **[Records](#records)**

All those types are generated with the following visibilities:
- `public`

Depending on the type, we generate a set of type members:
- **[Constructors](#constructors)**
- **[Fields](#fields)**
- **[Methods](#methods)**

When needed (fields, constructors params, methods params, ...), we use the following types:
- `int`
- `java.lang.Long`
- `java.util.Random`
- `java.sql.Time`
- `int[]`
- `java.lang.Long[]`
- `java.util.Random[]`
- `java.sql.Time[]`

When needed (constructors, methods, ...), we use the following exceptions:
- empty
- `java.io.IOException`

You can customize the API v1 generation with the following parameters:
- *typeHierarchyDepth*: Level of depth for the type hierarchy. (A extends B & B extends C is a depth of 2)
- *typeHierarchyWidth*: Level of width for the type hierarchy. (A implements B & C is a width of 2)
- *enumValuesCount*: Number of values for each enum.
- *paramsCount*: Number of parameters for each constructor and method.

### Classes

All constructors, fields and methods are generated with the following visibilities:
- `public`
- `protected`

For the fields, the modifiers combination are:
- empty
- `static`
- `final`
- `static final`

For the methods, the modifiers combination are:
- empty
- `static`
- `final`
- `static final`
- `synchronized`
- `static synchronized`
- `final synchronized`
- `static final synchronized`
- `abstract` (if class is abstract)

### Interfaces

All fields and methods are generated with the following visibilities:
- `public`

For the fields, the modifiers combination are:
- empty
- `static`
- `final`
- `static final`

For the methods, the modifiers combination are:
- empty
- `static`
- `abstract`
- `default`

### Enums

All fields and methods are generated with the following visibilities:
- `public`
- `protected`

For the fields, the modifiers combination are:
- empty
- `static`
- `final`
- `static final`

For the methods, the modifiers combination are:
- empty
- `static`
- `final`
- `static final`
- `synchronized`
- `static synchronized`
- `final synchronized`
- `static final synchronized`

### Records

All fields and methods are generated with the following visibilities:
- `public`
- `protected`

For the fields, the modifiers combination are:
- `static`
- `static final`

For the methods, the modifiers combination are:
- empty
- `static`
- `final`
- `static final`
- `synchronized`
- `static synchronized`
- `final synchronized`
- `static final synchronized`

### Fields

For one given visibility (_eg._ public) and modifiers combination (_eg._ final), we generate one field for each type, so we have 8 fields per configuration.

### Constructors

First, we always generate one public default constructor.

Then, we loop over the visibility, type (taking into account *paramsCount*) and exception to generate one constructor.

Finally, we repeat the same process by using a vararg as last parameter.

### Methods

For the return value, we use the previously presented types, but we also add:
- `void`

We loop over the visibility, set of possible modifiers, set of types to generate one return type, set of types to generate parameters (taking into account *paramsCount*) and exception to generate one method.

We repeat the same process by using a vararg as last parameter.

For the methods and constructors, we handle when it has already similar parameters.
For example, `void f(int ...a)` and `void f(int[] a)` are considered as the same method, so we do not generate the second one.

The full API generated is stored in the `<output_dir>/api` directory.

Currently, here are statistics of the API v1 generation:

| 136 types  | 136 |
|------------|-----|
| Classes    | 77  |
| Interfaces | 12  |
| Enums      | 5   |
| Records    | 42  |

| Type members | 36.794 |
|--------------|--------|
| Constructors | 137    |
| Methods      | 29.671 |
| Fields       | 6.976  |
| Enum values  | 10     |

## Client generation

Based on a previous work by [Monce _et al._](https://arxiv.org/pdf/2402.12024), we have a list of possible API client interactions.
We use this list to visit the API v1 and for each symbol, we generate all the valid interactions.
All the generated code is stored in only one file `FullClient.java` in the `<output_dir>/client` directory.

Currently, the client contains **47.776** lines of code.

## API v2 generation and evaluation

The purpose of this step is to apply one atomic change on the API v1, then evaluate the breaking changes detection tools against it.
We implemented 35 atomic changed (see below) that can be applied to the API v1.
Due to the high number of combinations, we can not keep all the new APIs v2 generated.
Thus, once the API v2 is generated, we compute the ground truth and evaluate the tools, then remove it.
The ground truth is computed by running the API client against the new API v2.

To generate one API v2, we are the *dumbest* as possible.
Let's take an example for the atomic change `Add%sModifierTo%s`:
- If we try to add the *abstract* modifier to a class that is already abstract, we consider it as an **impossible strategy** and no evaluation is done.
- Then, if it was a class not abstract, we add the *abstract* modifier to it, and we try to compile the API v2. Here, two cases can happen:
  - The API v2 does not compile, we consider it as an **error** strategy and no evaluation is done.
  - The API v2 compiles successfully, this strategy will be evaluated.

We have 3 output files:
- `<output_dir>/results/results-<timestamp>.csv`: Contains the results for each strategy applied.
- `<output_dir>/errors/errors-<timestamp>.csv`: Contains all strategies that raised an error during compilation.
- `<output_dir>/impossible_strategies/impossible_strategies-<timestamp>.csv`: Contains all strategies that are impossible to apply on the API v1.

Here are all 35 atomic changes that can be applied to the API v1:
- Add%sModifierTo%s
- AddException%sToConstructor%sIn%s
- AddException%sToMethod%sIn%s
- AddImplementedInterfaceToType%s
- AddMethodToType%s
- AddModifier%sToField%sIn%s
- AddModifier%sToMethod%sIn%s
- AddParameter%sToConstructor%sIn%s
- AddParameter%sToMethod%sIn%s
- AddRecordComponent%sToRecord%s
- AddSuperClassToClass%s
- ChangeField%sIn%sTypeTo%s
- ChangeMethod%sIn%sTypeTo%s
- ChangeParameter%sTo%sFromConstructor%sIn%s
- ChangeParameter%sTo%sFromMethod%sIn%s
- ChangeRecordComponent%sTo%sFromRecord%s
- ReduceConstructor%sIn%sVisibilityTo%s
- ReduceField%sIn%sVisibilityTo%s
- ReduceMethod%sIn%sVisibilityTo%s
- Reduce%sVisibilityTo%s
- Remove%sModifierIn%s
- RemoveConstructor%sIn%s
- RemoveEnumValue%sIn%s
- RemoveException%sFromConstructor%sIn%s
- RemoveException%sFromMethod%sIn%s
- RemoveField%sIn%s
- RemoveImplementedInterface%sFromType%s
- RemoveMethod%sIn%s
- RemoveModifier%sToField%sIn%s
- RemoveModifier%sToMethod%sIn%s
- RemoveParameter%sFromConstructor%sIn%s
- RemoveParameter%sFromMethod%sIn%s
- RemoveRecordComponent%sFromRecord%s
- RemoveSuperClassFromClass%s
- RemoveType%s

Currently, here are statistics on the strategies applied to the API v1:

| Strategies applied | 399.628 |
|--------------------|---------|
| Success            | 155.326 |
| Error              | 201.307 |
| Impossible         | 42.995  |

## Usage

To run the benchmark, you need to have [Maven](https://maven.apache.org/) installed on your machine.
Java 21 is required to run properly the benchmark.

From the `combinatorial` directory, first package the project:

```bash
./mvnw clean package -DskipTests
```

Then, you can access the CLI help:
```bash
java -jar target/roseau-combinatorial-0.3.0-SNAPSHOT-jar-with-dependencies.jar --help
```

## Results

For the last benchmark execution, we evaluated the following tools:
- [Roseau](../README.md)

Here are the short results of the evaluation:

| Tool   | Scope  | Accuracy | Precision | Recall | F1    |
|:-------|:-------|:---------|:----------|:-------|:------|
| Roseau | Global | 0.930    | 0.892     | 0.999  | 0.941 |
|        | Binary | 0.912    | 0.852     | 1.000  | 0.920 |
|        | Source | 0.851    | 0.766     | 0.992  | 0.864 |

You can find the full results of the benchmark execution in this [Jupyter Notebook](../result/results.ipynb).
