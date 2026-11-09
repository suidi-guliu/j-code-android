import unittest,tempfile
from pathlib import Path
from ui_scan import lex,sites,ROOT
class UiScanTest(unittest.TestCase):
    def scan(self,s):
        with tempfile.TemporaryDirectory(dir=ROOT/'localization') as d:
            p=Path(d)/'Ui.kt';p.write_text(s);return sites(p)
    def test_comments_never_become_visible_labels(self):
        self.assertEqual([],self.scan('// Text("Not UI")\n/* title = "Also not UI" */'))
    def test_named_parameter_keeps_the_first_literal(self):
        self.assertEqual('Visible title',self.scan('Dialog(title = "Visible title", value=7)')[0]['value'])
    def test_constant_sentence_fragments_merge(self):
        self.assertEqual('Full sentence.',self.scan('Text("Full " +\n "sentence.")')[0]['value'])
    def test_templates_with_nested_quotes_remain_one_token(self):
        _,tokens=lex('Text("Hello ${if(ok) "yes" else "no"}")')
        self.assertEqual(1,len(tokens));self.assertIn('"yes"',tokens[0][2])
    def test_resource_key_is_not_a_hardcoded_label(self):
        self.assertEqual([],self.scan('Text(strings.text("nav.files"))'))
    def test_calls_and_nested_arguments_stop_at_real_comma(self):
        rows=self.scan('ContextAction(icon(1,2), "Copy", enabled=true)')
        self.assertEqual(['Copy'],[r['value'] for r in rows])
if __name__=='__main__':unittest.main()
