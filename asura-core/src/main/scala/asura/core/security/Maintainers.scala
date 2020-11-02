package asura.core.security

case class Maintainers(
                        groups: Seq[PermissionItem],
                        projects: Seq[PermissionItem],
                        admins: Seq[String] = Nil
                      )