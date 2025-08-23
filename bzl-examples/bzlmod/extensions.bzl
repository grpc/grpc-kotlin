load("@aspect_rules_lint//format:repositories.bzl", "fetch_ktfmt")

def _install_ktfmt_impl(module_ctx):
    fetch_ktfmt()

install_ktfmt = module_extension(_install_ktfmt_impl)
