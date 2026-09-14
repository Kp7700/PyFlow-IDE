package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PyFileManager
import com.example.pyengine.PyFileIO
import com.example.pyengine.PyInterpreter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PyPocketEngineTest {

    private fun runPythonCode(code: String, inputs: List<String> = emptyList()): Pair<String, String> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.filesDir, "test_python_${System.nanoTime()}")
        testDir.mkdirs()
        val fileIO = PyFileIO(testDir)

        val stdout = StringBuilder()
        val stderr = StringBuilder()
        var inputIdx = 0

        val interpreter = PyInterpreter(
            fileIO = fileIO,
            stdout = { stdout.append(it) },
            stderr = { stderr.append(it) },
            onInputPrompt = {
                if (inputIdx < inputs.size) inputs[inputIdx++] else ""
            }
        )

        runBlocking {
            try {
                interpreter.execute(code, "test.py")
            } catch (e: Exception) {
                // error captured in stderr
            }
        }

        testDir.deleteRecursively()
        return Pair(stdout.toString(), stderr.toString())
    }

    @Test
    fun testBasicArithmeticAndVariables() {
        val code = """
            a = 10
            b = 20
            c = a + b * 2
            print("c =", c)
        """.trimIndent()

        val (out, err) = runPythonCode(code)
        assertTrue(err.isEmpty())
        assertEquals("c = 50\n", out)
    }

    @Test
    fun testLoopsAndFunctions() {
        val code = """
            def factorial(n):
                if n <= 1:
                    return 1
                return n * factorial(n - 1)
                
            results = [factorial(i) for i in range(1, 6)]
            print("results:", results)
        """.trimIndent()

        val (out, err) = runPythonCode(code)
        assertTrue(err.isEmpty())
        assertEquals("results: [1, 2, 6, 24, 120]\n", out)
    }

    @Test
    fun testMathAndRandomModules() {
        val code = """
            import math
            import random
            
            sq = math.sqrt(144)
            print(f"sqrt(144) = {int(sq)}")
            
            random.seed(42)
            val = random.randint(1, 100)
            print("random:", val > 0)
        """.trimIndent()

        val (out, err) = runPythonCode(code)
        assertTrue(err.isEmpty())
        assertTrue(out.contains("sqrt(144) = 12"))
        assertTrue(out.contains("random: True"))
    }

    @Test
    fun testOOPAndInheritance() {
        val code = """
            class Person:
                def __init__(self, name):
                    self.name = name
                    
                def greet(self):
                    return f"Hello, I am {self.name}"
                    
            class Student(Person):
                def __init__(self, name, grade):
                    super_name = name
                    self.name = super_name
                    self.grade = grade
                    
                def greet(self):
                    return f"Student {self.name} in Grade {self.grade}"
                    
            s = Student("Alice", 10)
            print(s.greet())
        """.trimIndent()

        val (out, err) = runPythonCode(code)
        assertTrue(err.isEmpty())
        assertEquals("Student Alice in Grade 10\n", out)
    }

    @Test
    fun testInteractiveInput() {
        val code = """
            name = input("Name: ")
            age = input("Age: ")
            print(f"User: {name}, Age: {age}")
        """.trimIndent()

        val (out, err) = runPythonCode(code, inputs = listOf("Developer", "28"))
        assertTrue(err.isEmpty())
        assertTrue(out.contains("User: Developer, Age: 28"))
    }

    @Test
    fun testFileReadWrite() {
        val code = """
            with open("data.txt", "w") as f:
                f.write("Line 1\nLine 2\n")
                
            with open("data.txt", "r") as f:
                content = f.read()
                
            print("Read:", content.strip().replace("\n", ", "))
        """.trimIndent()

        val (out, err) = runPythonCode(code)
        assertTrue(err.isEmpty())
        assertEquals("Read: Line 1, Line 2\n", out)
    }

    @Test
    fun testDataStructuresAndSlicing() {
        val code = """
            d = {"a": 1, "b": 2}
            d["c"] = 3
            print("keys:", sorted(list(d.keys())))
            
            s = "Python"
            print("reversed:", s[::-1])
        """.trimIndent()

        val (out, err) = runPythonCode(code)
        assertTrue(err.isEmpty())
        assertTrue(out.contains("keys: ['a', 'b', 'c']"))
        assertTrue(out.contains("reversed: nohtyP"))
    }

    @Test
    fun testFileManagerOperations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = PyFileManager(context)

        val created = manager.createFile("unit_test.py", "print('test')")
        assertTrue(created)

        val content = manager.readFile("unit_test.py")
        assertEquals("print('test')", content)

        val renamed = manager.renameFile("unit_test.py", "renamed_test.py")
        assertTrue(renamed)

        val deleted = manager.deleteFile("renamed_test.py")
        assertTrue(deleted)
    }
}
