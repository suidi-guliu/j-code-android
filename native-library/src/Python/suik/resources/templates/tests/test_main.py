import unittest
from main import add

class MainTests(unittest.TestCase):
    def test_adds_numbers(self):
        self.assertEqual(42, add(20, 22))
