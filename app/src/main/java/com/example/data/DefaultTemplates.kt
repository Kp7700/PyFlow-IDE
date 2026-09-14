package com.example.data

object DefaultTemplates {

    val MAIN_PY = """
# ==========================================
# Welcome to PyPocket - Python IDE & Runtime
# ==========================================
import math
import random
import time

print("🐍 Welcome to PyPocket Python IDE!")
print("🚀 Fast, offline Python 3 on Android\n")

# Basic math and calculations
a = 15
b = 27
total = a + b
print(f"Simple sum: {a} + {b} = {total}")

# Standard math module
radius = 5.0
area = math.pi * (radius ** 2)
print(f"Area of circle (r={radius}): {round(area, 3)}")

# Loops & Lists
squares = [x**2 for x in range(1, 6)]
print(f"Squares from 1 to 5: {squares}")

# Functions
def greet(user):
    return f"Hello, {user}! Ready to code Python on Android."

print(greet("Developer"))
print("-" * 40)
print("✨ Tap the Run button (▶) or switch files in the menu!")
""".trimIndent()

    val CALCULATOR_PY = """
# Interactive Calculator Demo
# Demonstrates functions, while loops, input(), and error handling

def add(x, y):
    return x + y

def subtract(x, y):
    return x - y

def multiply(x, y):
    return x * y

def divide(x, y):
    if y == 0:
        raise ZeroDivisionError("Cannot divide by zero!")
    return x / y

print("=" * 35)
print("       PyPocket Calculator")
print("=" * 35)

num1_str = input("Enter first number: ")
num2_str = input("Enter second number: ")
op = input("Choose operation (+, -, *, /): ")

try:
    n1 = float(num1_str)
    n2 = float(num2_str)
    
    if op == '+':
        res = add(n1, n2)
    elif op == '-':
        res = subtract(n1, n2)
    elif op == '*':
        res = multiply(n1, n2)
    elif op == '/':
        res = divide(n1, n2)
    else:
        res = "Unknown operation"
        
    print(f"\nResult: {n1} {op} {n2} = {res}")
except ZeroDivisionError as e:
    print(f"\nMath Error: {e}")
except Exception as e:
    print(f"\nInvalid input: {e}")
""".trimIndent()

    val INTERACTIVE_INPUT_PY = """
# Interactive User Profile Quiz
import random

print("=== Interactive Python Quiz ===")
name = input("What is your name? ")
age = input("How old are you? ")
hobby = input("What is your favorite hobby? ")

print("\n--- Profile Summary ---")
print(f"Name:  {name}")
print(f"Age:   {age}")
print(f"Hobby: {hobby}")

lucky_number = random.randint(1, 100)
print(f"\n✨ {name}, your lucky number today is: {lucky_number}!")
""".trimIndent()

    val DATA_STRUCTURES_PY = """
# Python Data Structures & Comprehensions

# 1. Lists & Comprehensions
numbers = [12, 5, 8, 19, 24, 3, 7]
evens = [x for x in numbers if x % 2 == 0]
print("Numbers:", numbers)
print("Even numbers:", evens)
print("Sorted numbers:", sorted(numbers))

# 2. Dictionaries
inventory = {
    "apples": 10,
    "bananas": 25,
    "oranges": 15
}
print("\nStore Inventory:")
for item, qty in inventory.items():
    print(f"  • {item}: {qty} in stock")

# 3. Sets (Unique items & operations)
set_a = {1, 2, 3, 4, 5}
set_b = {4, 5, 6, 7, 8}
print("\nSet Union:", set_a.union(set_b))
print("Set Intersection:", set_a.intersection(set_b))

# 4. Tuples & Slicing
rgb = (255, 128, 0)
print("\nColor Tuple:", rgb)
print("Slice [::-1]:", numbers[::-1])
""".trimIndent()

    val OOP_CLASSES_PY = """
# Object-Oriented Programming (OOP) in PyPocket

class Animal:
    def __init__(self, name, species):
        self.name = name
        self.species = species

    def speak(self):
        return f"{self.name} makes a sound."

class Dog(Animal):
    def __init__(self, name, breed):
        self.name = name
        self.species = "Canine"
        self.breed = breed

    def speak(self):
        return f"{self.name} the {self.breed} barks: Woof! Woof!"

class Cat(Animal):
    def __init__(self, name, color):
        self.name = name
        self.species = "Feline"
        self.color = color

    def speak(self):
        return f"{self.name} the {self.color} cat purrs: Meow~"

pets = [
    Dog("Buddy", "Golden Retriever"),
    Cat("Luna", "Calico"),
    Dog("Rocky", "German Shepherd")
]

print("=== Pets Parade ===")
for p in pets:
    print(p.speak())
""".trimIndent()

    val ALGORITHMS_PY = """
# Classic Algorithms & Benchmarks
import time

def fibonacci(n):
    if n <= 1:
        return n
    a, b = 0, 1
    for _ in range(2, n + 1):
        a, b = b, a + b
    return b

def is_prime(n):
    if n < 2:
        return False
    for i in range(2, int(n ** 0.5) + 1):
        if n % i == 0:
            return False
    return True

print("Computing Fibonacci terms:")
fib_sequence = [fibonacci(i) for i in range(15)]
print("First 15 terms:", fib_sequence)

print("\nFinding prime numbers up to 50:")
primes = [x for x in range(2, 50) if is_prime(x)]
print("Primes:", primes)

# Quick performance benchmark
start_t = time.perf_counter()
total = sum(x**2 for x in range(10000))
elapsed = time.perf_counter() - start_t
print(f"\nSum of 10,000 squares computed in {round(elapsed * 1000, 2)} ms")
""".trimIndent()

    val ALL_TEMPLATES = listOf(
        "main.py" to MAIN_PY,
        "calculator.py" to CALCULATOR_PY,
        "interactive_input.py" to INTERACTIVE_INPUT_PY,
        "data_structures.py" to DATA_STRUCTURES_PY,
        "oop_classes.py" to OOP_CLASSES_PY,
        "algorithms.py" to ALGORITHMS_PY
    )
}
