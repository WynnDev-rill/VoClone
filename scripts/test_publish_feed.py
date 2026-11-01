import importlib.util, pathlib, unittest
spec=importlib.util.spec_from_file_location('feed',pathlib.Path(__file__).with_name('publish-update-feed.py'))
feed=importlib.util.module_from_spec(spec);spec.loader.exec_module(feed)
class FeedTest(unittest.TestCase):
    def test_new_channel_and_forward_update(self):
        self.assertTrue(feed.next_feed(None,dict(versionCode=2,sha256='new')))
        self.assertTrue(feed.next_feed(dict(versionCode=1),dict(versionCode=2)))
    def test_repeating_exact_release_is_idempotent(self):
        version=dict(versionCode=2,sha256='same',sourceCommit='same')
        self.assertFalse(feed.next_feed(version,version.copy()))
    def test_rollback_and_same_version_mutation_cannot_overwrite_channel(self):
        with self.assertRaises(ValueError):feed.next_feed(dict(versionCode=3),dict(versionCode=2))
        with self.assertRaises(ValueError):feed.next_feed(dict(versionCode=2,sha256='original'),dict(versionCode=2,sha256='changed'))
        with self.assertRaises(ValueError):feed.next_feed(dict(versionCode=2,sourceCommit='original'),dict(versionCode=2,sourceCommit='changed'))
if __name__=='__main__':unittest.main()
