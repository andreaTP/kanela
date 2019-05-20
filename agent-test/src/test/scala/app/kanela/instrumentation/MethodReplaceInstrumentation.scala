package app.kanela.instrumentation

import app.kanela.cases.substitution.Pepe
import kanela.agent.api.instrumentation.InstrumentationBuilder

class MethodReplaceInstrumentation extends InstrumentationBuilder {
    onType("app.kanela.cases.substitution.Methods")
      .replace(method("awesomeMethod"), method("runAwesome"), classOf[Pepe].getDeclaredMethod("amazingMethod"))
}
